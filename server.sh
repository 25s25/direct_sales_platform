#!/bin/bash
# ============================================
# 直销系统后端服务管控脚本
# 功能：环境检测 / 启动 / 停止 / 重启 / 状态 / 性能监控 / 守护进程
# 用法：./server.sh {env|start|stop|restart|status|monitor|watch|help}
# ============================================

set -e

# ==================== 配置项 ====================
APP_NAME="ds-app"
APP_VERSION="1.0.0-SNAPSHOT"
JAR_FILE="$(cd "$(dirname "$0")" && pwd)/ds-backend/ds-app/target/${APP_NAME}-${APP_VERSION}.jar"
PID_FILE="/tmp/${APP_NAME}.pid"
LOG_DIR="$(cd "$(dirname "$0")" && pwd)/logs"
LOG_FILE="${LOG_DIR}/server.log"
GC_LOG_FILE="${LOG_DIR}/gc.log"
HEAP_DUMP_DIR="${LOG_DIR}/heapdump"
MONITOR_INTERVAL=10          # 性能监控间隔（秒）
WATCHDOG_CHECK=5             # 守护进程检测间隔（秒）
STARTUP_TIMEOUT=60           # 启动超时时间（秒）
GRACEFUL_TIMEOUT=30          # 优雅停止超时（秒）

# JVM 参数（根据服务器内存调整）
JAVA_OPTS="-server"
JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx2048m"
JAVA_OPTS="${JAVA_OPTS} -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m"
JAVA_OPTS="${JAVA_OPTS} -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
JAVA_OPTS="${JAVA_OPTS} -XX:+PrintGCDetails -XX:+PrintGCDateStamps"
JAVA_OPTS="${JAVA_OPTS} -Xlog:gc*:file=${GC_LOG_FILE}:time,uptime:filecount=10,filesize=50M"
JAVA_OPTS="${JAVA_OPTS} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${HEAP_DUMP_DIR}"
JAVA_OPTS="${JAVA_OPTS} -XX:+ExitOnOutOfMemoryError"
JAVA_OPTS="${JAVA_OPTS} -Djava.awt.headless=true"
JAVA_OPTS="${JAVA_OPTS} -Dfile.encoding=UTF-8"
JAVA_OPTS="${JAVA_OPTS} -Dspring.profiles.active=${SPRING_PROFILE:-dev}"

# ==================== 颜色输出 ====================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $(date '+%Y-%m-%d %H:%M:%S') $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $(date '+%Y-%m-%d %H:%M:%S') $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') $*"; }
log_title() { echo -e "${CYAN}$*${NC}"; }

# ==================== 环境检测 ====================
check_environment() {
    local errors=0

    log_title "============================================"
    log_title "  直销系统 - 环境检测"
    log_title "============================================"
    echo ""

    # JDK 检测
    log_info "检测 JDK ..."
    if command -v java &>/dev/null; then
        local java_ver=$(java -version 2>&1 | head -1 | awk -F '"' '{print $2}')
        local major_ver=$(echo "$java_ver" | cut -d'.' -f1)
        if [[ "$major_ver" -ge 17 ]]; then
            echo -e "  JDK 版本: ${GREEN}${java_ver} ✓${NC}"
        else
            echo -e "  JDK 版本: ${RED}${java_ver} ✗ (需要 JDK 17+)${NC}"
            ((errors++))
        fi
        echo "  路径: $(which java)"
    else
        echo -e "  ${RED}未检测到 Java 环境! ✗${NC}"
        ((errors++))
    fi
    echo ""

    # 内存检测
    log_info "检测系统内存 ..."
    local total_mem_kb=$(grep MemTotal /proc/meminfo 2>/dev/null | awk '{print $2}')
    if [[ -n "$total_mem_kb" ]]; then
        local total_mem_gb=$(echo "scale=1; $total_mem_kb / 1024 / 1024" | bc 2>/dev/null || echo "N/A")
        local avail_mem_kb=$(grep MemAvailable /proc/meminfo 2>/dev/null | awk '{print $2}')
        local avail_mem_gb=$(echo "scale=1; $avail_mem_kb / 1024 / 1024" | bc 2>/dev/null || echo "N/A")
        echo "  总内存: ${total_mem_gb}GB"
        echo "  可用内存: ${avail_mem_gb}GB"
        if [[ -n "$avail_mem_kb" && "$avail_mem_kb" -lt 1048576 ]]; then
            echo -e "  ${YELLOW}⚠ 可用内存不足 1GB，建议释放内存后启动${NC}"
        else
            echo -e "  内存状态: ${GREEN}充足 ✓${NC}"
        fi
    else
        echo "  ${YELLOW}无法获取内存信息（非 Linux 系统？）${NC}"
    fi
    echo ""

    # 磁盘检测
    log_info "检测磁盘空间 ..."
    local jar_dir=$(dirname "$JAR_FILE")
    local disk_avail=$(df -h "$jar_dir" 2>/dev/null | tail -1 | awk '{print $4}')
    local disk_use=$(df -h "$jar_dir" 2>/dev/null | tail -1 | awk '{print $5}')
    if [[ -n "$disk_avail" ]]; then
        echo "  日志目录: $LOG_DIR"
        echo "  可用空间: ${disk_avail}"
        echo "  已用比例: ${disk_use}"
    fi
    echo ""

    # JAR 文件检测
    log_info "检测 JAR 文件 ..."
    if [[ -f "$JAR_FILE" ]]; then
        local jar_size=$(ls -lh "$JAR_FILE" | awk '{print $5}')
        echo -e "  文件: ${GREEN}${JAR_FILE}${NC}"
        echo "  大小: ${jar_size}"
    else
        echo -e "  ${RED}JAR 文件不存在: ${JAR_FILE} ✗${NC}"
        echo -e "  ${YELLOW}请先执行: cd ds-backend && mvn clean package -DskipTests${NC}"
        ((errors++))
    fi
    echo ""

    # 端口检测
    log_info "检测端口占用 ..."
    local app_port=$(grep -E '^server\.port=' "$(dirname "$0")/ds-backend/ds-app/src/main/resources/application.yml" 2>/dev/null | awk -F'=' '{print $2}' | tr -d ' ')
    app_port=${app_port:-8080}
    if command -v ss &>/dev/null; then
        if ss -tlnp 2>/dev/null | grep -q ":${app_port} "; then
            echo -e "  端口 ${app_port}: ${YELLOW}已被占用 ⚠${NC}"
            ss -tlnp 2>/dev/null | grep ":${app_port} "
        else
            echo -e "  端口 ${app_port}: ${GREEN}可用 ✓${NC}"
        fi
    elif command -v netstat &>/dev/null; then
        if netstat -tlnp 2>/dev/null | grep -q ":${app_port} "; then
            echo -e "  端口 ${app_port}: ${YELLOW}已被占用 ⚠${NC}"
            netstat -tlnp 2>/dev/null | grep ":${app_port} "
        else
            echo -e "  端口 ${app_port}: ${GREEN}可用 ✓${NC}"
        fi
    else
        echo "  ${YELLOW}无法检测端口（ss/netstat 不可用）${NC}"
    fi
    echo ""

    # MySQL / Redis / RabbitMQ 连通性（可选）
    log_info "检测中间件连通性 ..."
    local mysql_host=$(grep -E '^\s*url:' "$(dirname "$0")/ds-backend/ds-app/src/main/resources/application.yml" 2>/dev/null | head -1 | sed 's/.*:\/\/\([^\/]*\)\/.*/\1/' | awk -F':' '{print $1}')
    local mysql_port=$(grep -E '^\s*url:' "$(dirname "$0")/ds-backend/ds-app/src/main/resources/application.yml" 2>/dev/null | head -1 | sed 's/.*:\/\/\([^\/]*\)\/.*/\1/' | awk -F':' '{print $2}')
    mysql_host=${mysql_host:-localhost}
    mysql_port=${mysql_port:-3306}
    if timeout 2 bash -c "echo >/dev/tcp/${mysql_host}/${mysql_port}" 2>/dev/null; then
        echo -e "  MySQL (${mysql_host}:${mysql_port}): ${GREEN}可达 ✓${NC}"
    else
        echo -e "  MySQL (${mysql_host}:${mysql_port}): ${YELLOW}不可达 ⚠${NC}"
    fi

    local redis_host=$(grep -E '^\s*host:' "$(dirname "$0")/ds-backend/ds-app/src/main/resources/application.yml" 2>/dev/null | head -1 | awk '{print $2}')
    redis_host=${redis_host:-localhost}
    if timeout 2 bash -c "echo >/dev/tcp/${redis_host}/6379" 2>/dev/null; then
        echo -e "  Redis (${redis_host}:6379): ${GREEN}可达 ✓${NC}"
    else
        echo -e "  Redis (${redis_host}:6379): ${YELLOW}不可达 ⚠${NC}"
    fi
    echo ""

    log_title "============================================"
    if [[ $errors -eq 0 ]]; then
        log_info "环境检测通过，可以启动服务"
    else
        log_error "环境检测发现 ${errors} 个问题，请修复后重试"
    fi
    log_title "============================================"
    return $errors
}

# ==================== 获取进程 PID ====================
get_pid() {
    if [[ -f "$PID_FILE" ]]; then
        local pid=$(cat "$PID_FILE" 2>/dev/null)
        if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
            echo "$pid"
            return 0
        fi
    fi
    # 备用：通过进程名查找
    local pid=$(pgrep -f "${APP_NAME}-${APP_VERSION}.jar" 2>/dev/null | head -1)
    if [[ -n "$pid" ]]; then
        echo "$pid"
        return 0
    fi
    return 1
}

# ==================== 启动 ====================
start() {
    log_title "============================================"
    log_title "  直销系统 - 启动服务"
    log_title "============================================"

    if get_pid &>/dev/null; then
        local pid=$(get_pid)
        log_warn "服务已在运行 (PID: ${pid})，无需重复启动"
        return 1
    fi

    if [[ ! -f "$JAR_FILE" ]]; then
        log_error "JAR 文件不存在: $JAR_FILE"
        log_info "请先执行: cd ds-backend && mvn clean package -DskipTests"
        return 1
    fi

    mkdir -p "$LOG_DIR" "$HEAP_DUMP_DIR"

    log_info "启动 JAR: $JAR_FILE"
    log_info "Spring Profile: ${SPRING_PROFILE:-dev}"
    log_info "JVM 参数: $JAVA_OPTS"
    log_info "日志文件: $LOG_FILE"

    nohup java ${JAVA_OPTS} -jar "$JAR_FILE" >> "$LOG_FILE" 2>&1 &
    local pid=$!
    echo "$pid" > "$PID_FILE"

    log_info "进程已启动 (PID: ${pid})，等待服务就绪 ..."

    # 等待启动完成
    local waited=0
    local app_port=$(grep -E '^server\.port=' "$(dirname "$0")/ds-backend/ds-app/src/main/resources/application.yml" 2>/dev/null | awk -F'=' '{print $2}' | tr -d ' ')
    app_port=${app_port:-8080}

    while [[ $waited -lt $STARTUP_TIMEOUT ]]; do
        if ! kill -0 "$pid" 2>/dev/null; then
            log_error "进程异常退出，请查看日志: tail -f $LOG_FILE"
            rm -f "$PID_FILE"
            return 1
        fi
        if timeout 1 bash -c "echo >/dev/tcp/localhost/${app_port}" 2>/dev/null; then
            log_info "服务启动成功! 端口: ${app_port}"
            log_info "PID: ${pid}"
            log_info "日志: tail -f ${LOG_FILE}"
            return 0
        fi
        sleep 2
        ((waited+=2))
        echo -n "."
    done
    echo ""

    if kill -0 "$pid" 2>/dev/null; then
        log_warn "服务启动超时（${STARTUP_TIMEOUT}秒），但进程仍在运行"
        log_warn "请手动检查日志: tail -f $LOG_FILE"
        return 0
    else
        log_error "服务启动失败"
        rm -f "$PID_FILE"
        return 1
    fi
}

# ==================== 停止 ====================
stop() {
    log_title "============================================"
    log_title "  直销系统 - 停止服务"
    log_title "============================================"

    if ! get_pid &>/dev/null; then
        log_warn "服务未在运行"
        rm -f "$PID_FILE"
        return 0
    fi

    local pid=$(get_pid)
    log_info "正在停止服务 (PID: ${pid}) ..."

    # 优雅关闭（先 SIGTERM）
    kill "$pid" 2>/dev/null

    local waited=0
    while [[ $waited -lt $GRACEFUL_TIMEOUT ]]; do
        if ! kill -0 "$pid" 2>/dev/null; then
            log_info "服务已优雅停止"
            rm -f "$PID_FILE"
            return 0
        fi
        sleep 1
        ((waited++))
    done

    # 强制关闭（SIGKILL）
    log_warn "优雅停止超时，执行强制关闭 ..."
    kill -9 "$pid" 2>/dev/null
    sleep 2

    if ! kill -0 "$pid" 2>/dev/null; then
        log_info "服务已强制停止"
    else
        log_error "无法停止进程 ${pid}"
        return 1
    fi

    rm -f "$PID_FILE"
    return 0
}

# ==================== 重启 ====================
restart() {
    log_title "============================================"
    log_title "  直销系统 - 重启服务"
    log_title "============================================"
    stop
    sleep 2
    start
}

# ==================== 状态查看 ====================
status() {
    log_title "============================================"
    log_title "  直销系统 - 运行状态"
    log_title "============================================"

    if ! get_pid &>/dev/null; then
        log_warn "服务未运行"
        echo ""
        echo "  JAR 文件: ${JAR_FILE}"
        echo "  JAR 存在: $( [[ -f "$JAR_FILE" ]] && echo '是' || echo '否' )"
        return 1
    fi

    local pid=$(get_pid)

    echo ""
    echo -e "  状态:      ${GREEN}运行中${NC}"
    echo "  PID:       ${pid}"
    echo "  JAR:       ${JAR_FILE}"

    # 运行时长
    local uptime=$(ps -p "$pid" -o etime= 2>/dev/null | tr -d ' ')
    echo "  运行时长:  ${uptime}"

    # 端口
    local port_info=$(ss -tlnp 2>/dev/null | grep "pid=${pid}" | awk '{print $4}' | head -1)
    if [[ -n "$port_info" ]]; then
        echo "  监听端口:  ${port_info}"
    fi

    # CPU 和内存
    if command -v ps &>/dev/null; then
        local cpu_mem=$(ps -p "$pid" -o %cpu,%mem,rss 2>/dev/null | tail -1)
        local cpu=$(echo "$cpu_mem" | awk '{print $1}')
        local mem=$(echo "$cpu_mem" | awk '{print $2}')
        local rss_kb=$(echo "$cpu_mem" | awk '{print $3}')
        if [[ -n "$rss_kb" ]]; then
            local rss_mb=$(echo "scale=1; $rss_kb / 1024" | bc 2>/dev/null || echo "N/A")
            echo "  CPU:       ${cpu}%"
            echo "  内存:      ${mem}% (RSS: ${rss_mb}MB)"
        fi
    fi

    # 线程数
    local threads=$(ps -p "$pid" -o nlwp= 2>/dev/null | tr -d ' ')
    echo "  线程数:    ${threads}"

    # 磁盘 I/O
    if [[ -f "/proc/${pid}/io" ]]; then
        local read_bytes=$(awk '/read_bytes/ {print $2}' "/proc/${pid}/io" 2>/dev/null)
        local write_bytes=$(awk '/write_bytes/ {print $2}' "/proc/${pid}/io" 2>/dev/null)
        if [[ -n "$read_bytes" ]]; then
            local read_mb=$(echo "scale=1; $read_bytes / 1048576" | bc 2>/dev/null || echo "N/A")
            local write_mb=$(echo "scale=1; $write_bytes / 1048576" | bc 2>/dev/null || echo "N/A")
            echo "  磁盘读取:  ${read_mb}MB"
            echo "  磁盘写入:  ${write_mb}MB"
        fi
    fi

    echo ""
    echo "  日志:       tail -f ${LOG_FILE}"
    echo "  GC 日志:    ${GC_LOG_FILE}"

    # 最近日志
    echo ""
    log_title "--- 最近 10 行日志 ---"
    if [[ -f "$LOG_FILE" ]]; then
        tail -10 "$LOG_FILE" | while IFS= read -r line; do echo "  $line"; done
    else
        echo "  (日志文件不存在)"
    fi

    return 0
}

# ==================== 性能监控 ====================
monitor() {
    log_title "============================================"
    log_title "  直销系统 - 性能监控 (${MONITOR_INTERVAL}秒/次, Ctrl+C 退出)"
    log_title "============================================"

    if ! get_pid &>/dev/null; then
        log_error "服务未运行，无法监控"
        return 1
    fi

    local pid=$(get_pid)
    log_info "监控进程 PID: ${pid}"

    # 打印表头
    printf "\n${CYAN}%-19s %6s %6s %8s %8s %6s %8s %8s${NC}\n" \
        "时间" "CPU%" "内存%" "RSS(MB)" "线程" "FD" "读(MB/s)" "写(MB/s)"
    printf "%.0s-" {1..85}
    echo ""

    local prev_read=0
    local prev_write=0
    local prev_time=$(date +%s)

    while true; do
        if ! kill -0 "$pid" 2>/dev/null; then
            log_error "进程已退出，监控停止"
            break
        fi

        local now=$(date +%s)
        local elapsed=$(( now - prev_time ))
        prev_time=$now

        local ts=$(date '+%Y-%m-%d %H:%M:%S')
        local stats=$(ps -p "$pid" -o %cpu,%mem,rss,nlwp= 2>/dev/null)
        local cpu=$(echo "$stats" | awk '{print $1}')
        local mem=$(echo "$stats" | awk '{print $2}')
        local rss_kb=$(echo "$stats" | awk '{print $3}')
        local threads=$(echo "$stats" | awk '{print $4}')
        local rss_mb=$(echo "scale=1; $rss_kb / 1024" | bc 2>/dev/null || echo "N/A")

        # 文件描述符
        local fd_count=$(ls /proc/${pid}/fd 2>/dev/null | wc -l)

        # 磁盘 I/O 速率
        local read_rate="N/A"
        local write_rate="N/A"
        if [[ -f "/proc/${pid}/io" ]]; then
            local cur_read=$(awk '/read_bytes/ {print $2}' "/proc/${pid}/io" 2>/dev/null)
            local cur_write=$(awk '/write_bytes/ {print $2}' "/proc/${pid}/io" 2>/dev/null)
            if [[ -n "$cur_read" && -n "$prev_read" && $elapsed -gt 0 ]]; then
                read_rate=$(echo "scale=2; ($cur_read - $prev_read) / $elapsed / 1048576" | bc 2>/dev/null || echo "N/A")
                write_rate=$(echo "scale=2; ($cur_write - $prev_write) / $elapsed / 1048576" | bc 2>/dev/null || echo "N/A")
            fi
            prev_read=$cur_read
            prev_write=$cur_write
        fi

        printf "%-19s %6s %6s %8s %8s %6s %8s %8s\n" \
            "$ts" "$cpu" "$mem" "$rss_mb" "$threads" "$fd_count" "$read_rate" "$write_rate"

        # 警告检查
        local rss_warn=$(echo "$rss_mb" | awk '{if($1+0 > 1800) print "1"}')
        if [[ "$rss_warn" == "1" ]]; then
            echo -e "  ${RED}⚠ 内存使用超过 1800MB，建议关注！${NC}"
        fi

        sleep "$MONITOR_INTERVAL"
    done
}

# ==================== 守护进程模式 ====================
watch() {
    log_title "============================================"
    log_title "  直销系统 - 守护进程模式"
    log_title "  检测间隔: ${WATCHDOG_CHECK}秒"
    log_title "  进程异常退出时将自动重启"
    log_title "  Ctrl+C 退出守护模式"
    log_title "============================================"

    trap 'log_info "守护进程已退出"; exit 0' INT TERM

    local restart_count=0
    local max_restart=10
    local restart_window=300  # 5分钟内最多重启10次

    while true; do
        if ! get_pid &>/dev/null; then
            ((restart_count++))
            log_error "检测到服务未运行，自动重启 (第 ${restart_count} 次)"

            # 防止无限重启
            if [[ $restart_count -gt $max_restart ]]; then
                log_error "5分钟内重启超过 ${max_restart} 次，可能存在严重问题，守护进程退出"
                log_error "请手动检查日志: tail -f $LOG_FILE"
                exit 1
            fi

            start
            if [[ $? -ne 0 ]]; then
                log_error "启动失败，${WATCHDOG_CHECK}秒后重试 ..."
            fi
        fi
        sleep "$WATCHDOG_CHECK"
    done
}

# ==================== 查看日志 ====================
logs() {
    local lines=${1:-50}
    if [[ -f "$LOG_FILE" ]]; then
        tail -n "$lines" "$LOG_FILE"
    else
        log_warn "日志文件不存在: $LOG_FILE"
    fi
}

# ==================== 健康检查接口 ====================
health() {
    local app_port=$(grep -E '^server\.port=' "$(dirname "$0")/ds-backend/ds-app/src/main/resources/application.yml" 2>/dev/null | awk -F'=' '{print $2}' | tr -d ' ')
    app_port=${app_port:-8080}

    log_info "健康检查: http://localhost:${app_port}/actuator/health"
    if command -v curl &>/dev/null; then
        curl -s "http://localhost:${app_port}/actuator/health" 2>/dev/null || echo "无法连接"
    else
        log_warn "curl 不可用，无法进行健康检查"
    fi
}

# ==================== 帮助信息 ====================
help() {
    echo ""
    log_title "  ╔══════════════════════════════════════╗"
    log_title "  ║     直销系统 - 服务管控脚本 v1.0     ║"
    log_title "  ╚══════════════════════════════════════╝"
    echo ""
    echo "  用法: ./server.sh <命令> [参数]"
    echo ""
    echo "  命令:"
    echo "    env        环境检测 - 检查 JDK/内存/端口/中间件"
    echo "    start      启动服务"
    echo "    stop       停止服务（先优雅再强制）"
    echo "    restart    重启服务"
    echo "    status     查看运行状态（PID/CPU/内存/线程/最新日志）"
    echo "    monitor    性能监控 - 实时显示 CPU/内存/线程/IO"
    echo "    watch      守护进程模式 - 自动检测并重启异常退出的服务"
    echo "    logs [N]   查看最近 N 行日志（默认 50）"
    echo "    health     健康检查"
    echo "    help       显示本帮助"
    echo ""
    echo "  示例:"
    echo "    ./server.sh env              # 启动前环境检查"
    echo "    ./server.sh start            # 启动服务"
    echo "    ./server.sh stop             # 停止服务"
    echo "    ./server.sh restart          # 重启服务"
    echo "    ./server.sh status           # 查看状态"
    echo "    ./server.sh monitor          # 实时性能监控"
    echo "    ./server.sh watch            # 守护进程模式（后台运行用 nohup）"
    echo "    ./server.sh logs 100         # 查看最近 100 行日志"
    echo ""
    echo "  环境变量:"
    echo "    SPRING_PROFILE   Spring 环境配置（默认: dev）"
    echo "                     用法: SPRING_PROFILE=prod ./server.sh start"
    echo ""
}

# ==================== 入口 ====================
case "${1:-help}" in
    env)
        check_environment
        ;;
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    monitor)
        monitor
        ;;
    watch)
        watch
        ;;
    logs)
        logs "${2:-50}"
        ;;
    health)
        health
        ;;
    help|--help|-h)
        help
        ;;
    *)
        echo -e "${RED}未知命令: $1${NC}"
        echo ""
        help
        exit 1
        ;;
esac