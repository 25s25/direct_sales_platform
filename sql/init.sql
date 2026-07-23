-- 直销系统核心表初始化脚本

-- ============================================
-- 1. 会员体系
-- ============================================

-- 会员等级表
CREATE TABLE IF NOT EXISTS ds_member_level (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name        VARCHAR(50)  NOT NULL COMMENT '等级名称',
    level_code  VARCHAR(20)  NOT NULL UNIQUE COMMENT '等级编码',
    sort_order  INT          DEFAULT 0 COMMENT '排序',
    condition_pv DECIMAL(18,2) DEFAULT 0 COMMENT '升级所需PV',
    discount_rate DECIMAL(5,2) DEFAULT 100.00 COMMENT '购物折扣率(%)',
    bonus_rate  DECIMAL(5,2) DEFAULT 0 COMMENT '奖金比例(%)',
    status      TINYINT      DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员等级表';

-- 会员主表
CREATE TABLE IF NOT EXISTS ds_member (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    member_no       VARCHAR(32)  NOT NULL UNIQUE COMMENT '会员编号',
    phone           VARCHAR(20)  NOT NULL UNIQUE COMMENT '手机号',
    password        VARCHAR(128) NOT NULL COMMENT '密码(加密)',
    real_name       VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    id_card         VARCHAR(18)  DEFAULT NULL COMMENT '身份证号',
    nickname        VARCHAR(100) DEFAULT NULL COMMENT '昵称',
    avatar          VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    email           VARCHAR(100) DEFAULT NULL UNIQUE COMMENT '邮箱',
    email_verified  TINYINT      DEFAULT 0 COMMENT '邮箱是否验证：0-否 1-是',
    phone_verified  TINYINT      DEFAULT 1 COMMENT '手机是否验证：0-否 1-是',
    level_id        BIGINT       DEFAULT NULL COMMENT '会员等级ID',
    recommend_id    BIGINT       DEFAULT NULL COMMENT '推荐人ID',
    parent_id       BIGINT       DEFAULT NULL COMMENT '安置人ID',
    position        TINYINT      DEFAULT 0 COMMENT '安置位置: 0-左区 1-右区',
    ancestor_path   VARCHAR(500) DEFAULT '' COMMENT '祖先路径(如: 1,5,23)',
    wallet_balance  DECIMAL(18,2) DEFAULT 0 COMMENT '钱包余额',
    frozen_amount   DECIMAL(18,2) DEFAULT 0 COMMENT '冻结金额',
    total_pv        DECIMAL(18,2) DEFAULT 0 COMMENT '累计PV',
    status          TINYINT      DEFAULT 1 COMMENT '状态: 0-禁用 1-正常 2-待审核',
    version         INT          DEFAULT 0 COMMENT '乐观锁版本号',
    register_ip     VARCHAR(50)  DEFAULT NULL COMMENT '注册IP',
    last_login_time DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_recommend_id (recommend_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_level_id (level_id),
    INDEX idx_ancestor_path (ancestor_path),
    INDEX idx_member_no (member_no),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员主表';

-- 会员关系路径表（辅助查询）
CREATE TABLE IF NOT EXISTS ds_member_path (
    ancestor_id    BIGINT NOT NULL COMMENT '祖先ID',
    descendant_id  BIGINT NOT NULL COMMENT '后代ID',
    depth          INT    NOT NULL DEFAULT 0 COMMENT '层级深度',
    PRIMARY KEY (ancestor_id, descendant_id),
    INDEX idx_descendant (descendant_id, depth)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员关系路径表';

-- 会员操作日志
CREATE TABLE IF NOT EXISTS ds_member_log (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    member_id   BIGINT       NOT NULL COMMENT '会员ID',
    action      VARCHAR(50)  NOT NULL COMMENT '操作类型',
    content     TEXT         COMMENT '操作内容',
    ip          VARCHAR(50)  COMMENT '操作IP',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_member_id (member_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员操作日志';

-- 会员第三方账号绑定表
CREATE TABLE IF NOT EXISTS ds_member_social (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    member_id   BIGINT       NOT NULL COMMENT '会员ID',
    social_type VARCHAR(20)  NOT NULL COMMENT 'wechat_web/wechat_mp/wechat_miniapp/workwechat',
    union_id    VARCHAR(100) COMMENT '微信unionid',
    open_id     VARCHAR(100) NOT NULL COMMENT '平台openid',
    nickname    VARCHAR(100) COMMENT '昵称',
    avatar      VARCHAR(500) COMMENT '头像',
    raw_data    JSON         COMMENT '原始用户信息',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_type_openid (social_type, open_id),
    INDEX idx_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员第三方账号绑定表';

-- ============================================
-- 2. 商品体系
-- ============================================

-- 商品分类
CREATE TABLE IF NOT EXISTS ds_product_category (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    parent_id   BIGINT       DEFAULT 0 COMMENT '父分类ID',
    name        VARCHAR(100) NOT NULL COMMENT '分类名称',
    sort_order  INT          DEFAULT 0 COMMENT '排序',
    icon        VARCHAR(255) DEFAULT NULL COMMENT '图标',
    status      TINYINT      DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 商品主表
CREATE TABLE IF NOT EXISTS ds_product (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    product_no      VARCHAR(32)  NOT NULL UNIQUE COMMENT '商品编号',
    category_id     BIGINT       NOT NULL COMMENT '分类ID',
    name            VARCHAR(200) NOT NULL COMMENT '商品名称',
    subtitle        VARCHAR(200) DEFAULT NULL COMMENT '副标题',
    main_image      VARCHAR(255) DEFAULT NULL COMMENT '主图',
    images          TEXT         COMMENT '商品图片(JSON数组)',
    detail          LONGTEXT     COMMENT '商品详情',
    retail_price    DECIMAL(18,2) NOT NULL COMMENT '零售价',
    member_price    DECIMAL(18,2) DEFAULT NULL COMMENT '会员价',
    pv              DECIMAL(18,2) DEFAULT 0 COMMENT 'PV值',
    stock           INT          DEFAULT 0 COMMENT '库存',
    sales_count     INT          DEFAULT 0 COMMENT '销量',
    is_recommend    TINYINT      DEFAULT 0 COMMENT '是否推荐: 0-否 1-是',
    is_new          TINYINT      DEFAULT 0 COMMENT '是否新品: 0-否 1-是',
    status          TINYINT      DEFAULT 1 COMMENT '状态: 0-下架 1-上架',
    version         INT          DEFAULT 0 COMMENT '乐观锁版本号',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category_id (category_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品主表';

-- 商品SKU表
CREATE TABLE IF NOT EXISTS ds_product_sku (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    product_id    BIGINT       NOT NULL COMMENT '商品ID',
    sku_code      VARCHAR(50)  NOT NULL UNIQUE COMMENT 'SKU编码',
    spec_name     VARCHAR(200) DEFAULT NULL COMMENT '规格名称',
    spec_value    VARCHAR(200) DEFAULT NULL COMMENT '规格值',
    price         DECIMAL(18,2) NOT NULL COMMENT '价格',
    stock         INT          DEFAULT 0 COMMENT '库存',
    status        TINYINT      DEFAULT 1,
    version       INT          DEFAULT 0 COMMENT '乐观锁版本号',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU表';

-- ============================================
-- 3. 订单体系
-- ============================================

-- 订单主表
CREATE TABLE IF NOT EXISTS ds_order (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(32)  NOT NULL UNIQUE COMMENT '订单编号',
    member_id       BIGINT       NOT NULL COMMENT '下单会员ID',
    total_amount    DECIMAL(18,2) NOT NULL COMMENT '订单总金额',
    pay_amount      DECIMAL(18,2) DEFAULT 0 COMMENT '实付金额',
    discount_amount DECIMAL(18,2) DEFAULT 0 COMMENT '优惠金额',
    total_pv        DECIMAL(18,2) DEFAULT 0 COMMENT '订单总PV',
    receiver_name   VARCHAR(50)  DEFAULT NULL COMMENT '收货人',
    receiver_phone  VARCHAR(20)  DEFAULT NULL COMMENT '收货电话',
    receiver_addr   VARCHAR(500) DEFAULT NULL COMMENT '收货地址',
    pay_type        TINYINT      DEFAULT NULL COMMENT '支付方式: 1-微信 2-支付宝 3-余额',
    pay_time        DATETIME     DEFAULT NULL COMMENT '支付时间',
    ship_time       DATETIME     DEFAULT NULL COMMENT '发货时间',
    express_company VARCHAR(100) DEFAULT NULL COMMENT '物流公司',
    express_no      VARCHAR(100) DEFAULT NULL COMMENT '物流单号',
    receive_time    DATETIME     DEFAULT NULL COMMENT '签收时间',
    status          TINYINT      DEFAULT 0 COMMENT '状态: 0-待付款 1-已付款 2-已发货 3-已签收 4-已完成 5-已取消',
    remark          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_member_id (member_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- 订单明细表
CREATE TABLE IF NOT EXISTS ds_order_item (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    order_id    BIGINT       NOT NULL COMMENT '订单ID',
    product_id  BIGINT       NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    product_image VARCHAR(255) DEFAULT NULL COMMENT '商品图片',
    sku_code    VARCHAR(50)  DEFAULT NULL COMMENT 'SKU编码',
    spec_name   VARCHAR(200) DEFAULT NULL COMMENT '规格名称',
    price       DECIMAL(18,2) NOT NULL COMMENT '单价',
    quantity    INT          NOT NULL DEFAULT 1 COMMENT '数量',
    pv          DECIMAL(18,2) DEFAULT 0 COMMENT '单品PV',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 订单业绩归属表
CREATE TABLE IF NOT EXISTS ds_order_bonus (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    order_id    BIGINT       NOT NULL COMMENT '订单ID',
    member_id   BIGINT       NOT NULL COMMENT '业绩归属会员ID',
    relation_type VARCHAR(20) NOT NULL COMMENT '关系类型: recommend-推荐 parent-安置',
    bonus_level INT          DEFAULT 0 COMMENT '奖金层级(1级/2级/3级)',
    pv          DECIMAL(18,2) DEFAULT 0 COMMENT '该级PV',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id),
    INDEX idx_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单业绩归属表';

-- 售后退货单表
CREATE TABLE IF NOT EXISTS ds_order_return (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    return_no       VARCHAR(32)  NOT NULL UNIQUE COMMENT '退货单号',
    order_id        BIGINT       NOT NULL COMMENT '订单ID',
    member_id       BIGINT       NOT NULL COMMENT '会员ID',
    reason          VARCHAR(500) DEFAULT NULL COMMENT '退货原因',
    refund_amount   DECIMAL(18,2) NOT NULL COMMENT '退款金额',
    status          TINYINT      DEFAULT 0 COMMENT '状态: 0-待审核 1-审核通过 2-审核驳回',
    audit_remark    VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
    audit_time      DATETIME     DEFAULT NULL COMMENT '审核时间',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id),
    INDEX idx_member_id (member_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后退货单表';

-- ============================================
-- 4. 奖金体系
-- ============================================

-- 奖金制度配置表
CREATE TABLE IF NOT EXISTS ds_bonus_plan (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL COMMENT '制度名称',
    plan_type       VARCHAR(30)  NOT NULL COMMENT '制度类型: DIFFERENTIAL/BINARY/MATRIX/SUNRAY',
    calc_rule_json  JSON         COMMENT '计算规则配置(JSON)',
    is_active       TINYINT      DEFAULT 0 COMMENT '是否当前启用: 0-否 1-是',
    remark          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖金制度配置表';

-- 奖金结算记录表
CREATE TABLE IF NOT EXISTS ds_bonus_record (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    member_id   BIGINT       NOT NULL COMMENT '会员ID',
    period      VARCHAR(20)  NOT NULL COMMENT '结算周期(如: 2026-06)',
    bonus_type  VARCHAR(30)  NOT NULL COMMENT '奖金类型: RETAIL-零售奖 RECOMMEND-推荐奖 MATCH-对碰奖 LAYER-层奖 LEADER-领导奖',
    amount      DECIMAL(18,2) NOT NULL COMMENT '奖金金额',
    source_order_id BIGINT   DEFAULT NULL COMMENT '来源订单ID',
    source_order_no VARCHAR(64) DEFAULT NULL COMMENT '来源订单编号',
    status      TINYINT      DEFAULT 0 COMMENT '状态: 0-待发放 1-已发放 2-已取消',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    grant_time  DATETIME     DEFAULT NULL COMMENT '发放时间',
    INDEX idx_member_id (member_id),
    INDEX idx_period (period),
    INDEX idx_bonus_type (bonus_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖金结算记录表';

-- 电子钱包流水表
CREATE TABLE IF NOT EXISTS ds_wallet_log (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    member_id       BIGINT       NOT NULL COMMENT '会员ID',
    log_type        VARCHAR(30)  NOT NULL COMMENT '类型: BONUS-奖金 INCOME-收入 WITHDRAW-提现 REFUND-退款',
    amount          DECIMAL(18,2) NOT NULL COMMENT '金额(正数为入账,负数为出账)',
    balance_before  DECIMAL(18,2) NOT NULL COMMENT '变动前余额',
    balance_after   DECIMAL(18,2) NOT NULL COMMENT '变动后余额',
    reference_id    BIGINT       DEFAULT NULL COMMENT '关联业务ID',
    remark          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_member_id (member_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子钱包流水表';

-- 提现申请表
CREATE TABLE IF NOT EXISTS ds_withdraw (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    member_id   BIGINT       NOT NULL COMMENT '会员ID',
    withdraw_no VARCHAR(32)  NOT NULL UNIQUE COMMENT '提现单号',
    amount      DECIMAL(18,2) NOT NULL COMMENT '提现金额',
    fee         DECIMAL(18,2) DEFAULT 0 COMMENT '手续费',
    actual_amount DECIMAL(18,2) DEFAULT 0 COMMENT '实际到账金额',
    bank_name   VARCHAR(100) DEFAULT NULL COMMENT '银行名称',
    bank_card   VARCHAR(30)  DEFAULT NULL COMMENT '银行卡号',
    status      TINYINT      DEFAULT 0 COMMENT '状态: 0-待审核 1-审核通过 2-已打款 3-已驳回',
    audit_time  DATETIME     DEFAULT NULL COMMENT '审核时间',
    grant_time  DATETIME     DEFAULT NULL COMMENT '打款时间',
    remark      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_member_id (member_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现申请表';

-- ============================================
-- 5. 系统管理
-- ============================================

-- 系统用户表
CREATE TABLE IF NOT EXISTS ds_sys_user (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(128) NOT NULL COMMENT '密码(加密)',
    real_name   VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像',
    status      TINYINT      DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS ds_sys_role (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL UNIQUE COMMENT '角色名称',
    role_code   VARCHAR(50)  NOT NULL UNIQUE COMMENT '角色编码',
    remark      VARCHAR(200) DEFAULT NULL COMMENT '备注',
    status      TINYINT      DEFAULT 1,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS ds_sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户角色关联表';

-- 系统菜单表
CREATE TABLE IF NOT EXISTS ds_sys_menu (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    parent_id   BIGINT       DEFAULT 0 COMMENT '父菜单ID',
    name        VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    title       VARCHAR(50)  NOT NULL COMMENT '菜单标题',
    path        VARCHAR(200) DEFAULT NULL COMMENT '路由路径',
    component   VARCHAR(200) DEFAULT NULL COMMENT '组件路径',
    icon        VARCHAR(100) DEFAULT NULL COMMENT '图标',
    sort_order  INT          DEFAULT 0 COMMENT '排序',
    menu_type   TINYINT      DEFAULT 1 COMMENT '类型: 1-菜单 2-按钮',
    permission  VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    status      TINYINT      DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent_id (parent_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单表';

-- 系统参数配置表
CREATE TABLE IF NOT EXISTS ds_sys_config (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    config_key  VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT        COMMENT '配置值',
    remark      VARCHAR(200) DEFAULT NULL COMMENT '备注',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数配置表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS ds_sys_log (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       DEFAULT NULL COMMENT '操作用户ID',
    username    VARCHAR(50)  DEFAULT NULL COMMENT '操作用户名',
    module      VARCHAR(50)  DEFAULT NULL COMMENT '操作模块',
    action      VARCHAR(50)  NOT NULL COMMENT '操作类型',
    description VARCHAR(500) DEFAULT NULL COMMENT '操作描述',
    ip          VARCHAR(50)  DEFAULT NULL COMMENT '操作IP',
    request_url VARCHAR(255) DEFAULT NULL COMMENT '请求URL',
    request_params TEXT      COMMENT '请求参数',
    cost_time   BIGINT       DEFAULT 0 COMMENT '耗时(ms)',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';

-- ============================================
-- 6. 扩展功能：文件上传
-- ============================================

-- 文件上传记录表
CREATE TABLE IF NOT EXISTS ds_file_record (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    file_name    VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_type    VARCHAR(50)  COMMENT 'MIME类型',
    file_size    BIGINT       COMMENT '文件大小（字节）',
    storage_type VARCHAR(20)  NOT NULL COMMENT 'local/aliyun/tencent/qiniu',
    file_path    VARCHAR(500) NOT NULL COMMENT '存储路径/Key',
    access_url   VARCHAR(500) COMMENT '访问URL',
    module       VARCHAR(50)  COMMENT '业务模块：product/avatar/idcard/...',
    biz_id       BIGINT       COMMENT '业务ID',
    create_by    BIGINT       COMMENT '上传者ID',
    deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_module_biz (module, biz_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件上传记录表';

-- ============================================
-- 7. 扩展功能：在线支付
-- ============================================

-- 支付流水表
CREATE TABLE IF NOT EXISTS ds_payment_order (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no        VARCHAR(32)  NOT NULL COMMENT '业务订单号',
    pay_order_no    VARCHAR(64)  NOT NULL UNIQUE COMMENT '支付流水号',
    member_id       BIGINT       NOT NULL COMMENT '会员ID',
    amount          DECIMAL(18,2) NOT NULL COMMENT '支付金额',
    channel VARCHAR(20) NOT NULL DEFAULT '' COMMENT '支付渠道',
    status          TINYINT      DEFAULT 0 COMMENT '0-待支付 1-支付中 2-成功 3-失败 4-已退款',
    third_order_no  VARCHAR(100) COMMENT '第三方支付单号',
    pay_time        DATETIME     COMMENT '支付完成时间',
    expire_time     DATETIME     COMMENT '订单过期时间',
    callback_count  INT          DEFAULT 0 COMMENT '回调次数',
    callback_result TEXT         COMMENT '最后一次回调内容',
    extra_data      JSON         COMMENT '渠道特有扩展数据',
    deleted         TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pay_order_no (pay_order_no),
    INDEX idx_order_no (order_no),
    INDEX idx_member_id (member_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

-- ============================================
-- 8. 初始化数据
-- ============================================

-- 默认会员等级
INSERT INTO ds_member_level (name, level_code, sort_order, condition_pv, discount_rate, bonus_rate) VALUES
('普通会员', 'NORMAL', 1, 0, 100.00, 5.00),
('银卡会员', 'SILVER', 2, 100, 95.00, 10.00),
('金卡会员', 'GOLD', 3, 500, 90.00, 15.00),
('钻石会员', 'DIAMOND', 4, 2000, 85.00, 20.00),
('皇冠会员', 'CROWN', 5, 10000, 80.00, 25.00);

-- 默认管理员
INSERT INTO ds_sys_user (username, password, real_name, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '系统管理员', 1);

-- 默认角色
INSERT INTO ds_sys_role (name, role_code, remark) VALUES
('超级管理员', 'SUPER_ADMIN', '系统超级管理员，拥有所有权限'),
('运营管理员', 'ADMIN', '运营管理员'),
('财务管理员', 'FINANCE', '财务管理权限');

-- 超级管理员绑定角色
INSERT INTO ds_sys_user_role (user_id, role_id) VALUES (1, 1);

-- 默认系统菜单
INSERT INTO ds_sys_menu (id, parent_id, name, title, path, component, icon, sort_order, menu_type, permission, status) VALUES
(1, 0, 'Dashboard', '数据概览', '/admin/dashboard', 'admin/dashboard', 'DataLine', 1, 1, 'report:view', 1),
(2, 0, 'MemberManage', '会员管理', '/admin/member', 'admin/member', 'User', 2, 1, 'member:manage', 1),
(3, 0, 'ProductManage', '商品管理', '/admin/product', 'admin/product', 'Goods', 3, 1, 'product:manage', 1),
(4, 0, 'OrderManage', '订单管理', '/admin/order', 'admin/order', 'ShoppingBag', 4, 1, 'order:manage', 1),
(5, 0, 'BonusManage', '奖金管理', '/admin/bonus', 'admin/bonus', 'Money', 5, 1, 'bonus:manage', 1),
(6, 0, 'FinanceManage', '财务管理', '/admin/finance', 'admin/finance', 'Wallet', 6, 1, 'finance:manage', 1),
(7, 0, 'SystemManage', '系统管理', '/admin/system', 'admin/system', 'Setting', 7, 1, 'system:manage', 1)
ON DUPLICATE KEY UPDATE title = VALUES(title), path = VALUES(path), component = VALUES(component);

-- 默认奖金制度（级差制）
INSERT INTO ds_bonus_plan (name, plan_type, calc_rule_json, is_active, remark) VALUES
('标准级差制', 'DIFFERENTIAL',
 '{"levels":[{"level":"NORMAL","rate":5},{"level":"SILVER","rate":10},{"level":"GOLD","rate":15},{"level":"DIAMOND","rate":20},{"level":"CROWN","rate":25}],"maxDepth":3}',
 1, '默认级差奖金制度');

-- 文件上传配置
INSERT INTO ds_sys_config (config_key, config_value, remark, create_time, update_time) VALUES
('oss.enabled', 'false', '是否启用文件上传', NOW(), NOW()),
('oss.type', 'local', '存储类型：local/aliyun/tencent/qiniu', NOW(), NOW()),
('oss.max-file-size', '10485760', '单文件最大限制（字节）', NOW(), NOW()),
('oss.allowed-types', 'jpg,jpeg,png,gif,bmp,webp,mp4,mp3,doc,docx,xls,xlsx,pdf', '允许的文件扩展名', NOW(), NOW()),
('oss.local.base-path', '/data/uploads', '本地存储根目录', NOW(), NOW()),
('oss.local.base-url', 'http://localhost:8080/uploads', '本地访问基础URL', NOW(), NOW()),
('oss.aliyun.endpoint', '', 'OSS Endpoint', NOW(), NOW()),
('oss.aliyun.bucket', '', 'OSS Bucket', NOW(), NOW()),
('oss.aliyun.access-key', '', 'OSS AccessKeyId', NOW(), NOW()),
('oss.aliyun.secret-key', '', 'OSS AccessKeySecret', NOW(), NOW()),
('oss.tencent.region', '', 'COS Region', NOW(), NOW()),
('oss.tencent.bucket', '', 'COS Bucket', NOW(), NOW()),
('oss.tencent.secret-id', '', 'COS SecretId', NOW(), NOW()),
('oss.tencent.secret-key', '', 'COS SecretKey', NOW(), NOW()),
('oss.qiniu.domain', '', '七牛域名', NOW(), NOW()),
('oss.qiniu.bucket', '', '七牛 Bucket', NOW(), NOW()),
('oss.qiniu.access-key', '', '七牛 AccessKey', NOW(), NOW()),
('oss.qiniu.secret-key', '', '七牛 SecretKey', NOW(), NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 支付配置
INSERT INTO ds_sys_config (config_key, config_value, remark, create_time, update_time) VALUES
('pay.alipay.enabled', 'false', '是否启用支付宝', NOW(), NOW()),
('pay.alipay.app-id', '', '支付宝 AppID', NOW(), NOW()),
('pay.alipay.merchant-private-key', '', '支付宝应用私钥', NOW(), NOW()),
('pay.alipay.alipay-public-key', '', '支付宝公钥', NOW(), NOW()),
('pay.alipay.gateway', 'https://openapi.alipay.com/gateway.do', '支付宝网关', NOW(), NOW()),
('pay.alipay.notify-url', 'http://localhost:8080/api/pay/callback/alipay', '支付宝异步回调地址', NOW(), NOW()),
('pay.alipay.return-url', 'http://localhost:3000/shop/pay-result', '支付宝同步返回地址', NOW(), NOW()),
('pay.alipay.f2f.enabled', 'false', '是否启用支付宝当面付', NOW(), NOW()),
('pay.alipay.f2f.qr-timeout', '120', '支付宝当面付二维码有效期（分钟）', NOW(), NOW()),
('pay.alipay.web.enabled', 'false', '是否启用支付宝网页支付', NOW(), NOW()),
('pay.alipay.web.product-code', 'FAST_INSTANT_TRADE_PAY', '支付宝网页支付产品码', NOW(), NOW()),
('pay.wechat.enabled', 'false', '是否启用微信支付', NOW(), NOW()),
('pay.wechat.mchid', '', '微信商户号', NOW(), NOW()),
('pay.wechat.notify-url', 'http://localhost:8080/api/pay/callback/wechat', '微信异步回调地址', NOW(), NOW()),
('pay.wechat.api-v3-key', '', '微信APIv3密钥', NOW(), NOW()),
('pay.wechat.cert-serial-no', '', '微信商户证书序列号', NOW(), NOW()),
('pay.wechat.private-key', '', '微信商户私钥PEM', NOW(), NOW()),
('pay.wechat.native.enabled', 'false', '是否启用微信Native支付', NOW(), NOW()),
('pay.wechat.native.app-id', '', '微信Native支付AppID', NOW(), NOW()),
('pay.wechat.jsapi.enabled', 'false', '是否启用微信公众号支付', NOW(), NOW()),
('pay.wechat.jsapi.app-id', '', '微信公众号AppID', NOW(), NOW()),
('pay.wechat.jsapi.secret', '', '微信公众号Secret', NOW(), NOW()),
('pay.wechat.miniapp.enabled', 'false', '是否启用微信小程序支付', NOW(), NOW()),
('pay.wechat.miniapp.app-id', '', '微信小程序AppID', NOW(), NOW()),
('pay.wechat.miniapp.secret', '', '微信小程序Secret', NOW(), NOW()),
('pay.paypal.enabled', 'false', '是否启用PayPal', NOW(), NOW()),
('pay.paypal.client-id', '', 'PayPal Client ID', NOW(), NOW()),
('pay.paypal.client-secret', '', 'PayPal Client Secret', NOW(), NOW()),
('pay.paypal.mode', 'sandbox', 'PayPal模式：sandbox/live', NOW(), NOW()),
('pay.paypal.return-url', 'http://localhost:3000/shop/pay-result', 'PayPal返回地址', NOW(), NOW()),
('pay.paypal.cancel-url', 'http://localhost:3000/shop/pay-cancel', 'PayPal取消地址', NOW(), NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 短信/邮件配置
INSERT INTO ds_sys_config (config_key, config_value, remark, create_time, update_time) VALUES
('message.email.enabled', 'false', '是否启用邮件', NOW(), NOW()),
('message.email.host', '', 'SMTP服务器', NOW(), NOW()),
('message.email.port', '465', 'SMTP端口', NOW(), NOW()),
('message.email.username', '', '发件邮箱', NOW(), NOW()),
('message.email.password', '', '发件邮箱授权码', NOW(), NOW()),
('message.email.from-name', '直销系统', '发件人名称', NOW(), NOW()),
('message.sms.enabled', 'false', '是否启用短信', NOW(), NOW()),
('message.sms.aliyun.access-key', '', '阿里云AccessKeyId', NOW(), NOW()),
('message.sms.aliyun.secret-key', '', '阿里云AccessKeySecret', NOW(), NOW()),
('message.sms.aliyun.sign-name', '', '短信签名', NOW(), NOW()),
('message.sms.aliyun.template-code', '', '短信模板CODE', NOW(), NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 社交登录配置
INSERT INTO ds_sys_config (config_key, config_value, remark, create_time, update_time) VALUES
('social.wechat.web.enabled', 'false', '是否启用微信网页扫码登录', NOW(), NOW()),
('social.wechat.web.app-id', '', '微信开放平台AppID', NOW(), NOW()),
('social.wechat.web.secret', '', '微信开放平台Secret', NOW(), NOW()),
('social.wechat.mp.enabled', 'false', '是否启用微信公众号登录', NOW(), NOW()),
('social.wechat.mp.app-id', '', '微信公众号AppID', NOW(), NOW()),
('social.wechat.mp.secret', '', '微信公众号Secret', NOW(), NOW()),
('social.wechat.miniapp.enabled', 'false', '是否启用微信小程序登录', NOW(), NOW()),
('social.wechat.miniapp.app-id', '', '微信小程序AppID', NOW(), NOW()),
('social.wechat.miniapp.secret', '', '微信小程序Secret', NOW(), NOW()),
('social.wechat.redirect-uri', 'http://localhost:3000/auth/social-callback', '微信统一回调地址', NOW(), NOW()),
('social.workwechat.enabled', 'false', '是否启用企业微信登录', NOW(), NOW()),
('social.workwechat.corp-id', '', '企业微信CorpID', NOW(), NOW()),
('social.workwechat.agent-id', '', '企业微信AgentID', NOW(), NOW()),
('social.workwechat.secret', '', '企业微信Secret', NOW(), NOW()),
('social.workwechat.redirect-uri', 'http://localhost:3000/auth/social-callback', '企业微信回调地址', NOW(), NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

-- ============================================
-- 9. 扩展功能：权限体系
-- ============================================

-- 权限表
CREATE TABLE IF NOT EXISTS ds_sys_permission (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL COMMENT '权限名称',
    permission_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    remark      VARCHAR(200) DEFAULT NULL COMMENT '备注',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS ds_sys_role_permission (
    role_id       BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 默认权限
INSERT INTO ds_sys_permission (name, permission_code, remark) VALUES
('系统管理', 'system:manage', '系统管理超级权限'),
('报表查看', 'report:view', '查看数据报表'),
('用户查看', 'system:user:view', '查看系统用户'),
('用户新增', 'system:user:add', '新增系统用户'),
('用户编辑', 'system:user:edit', '编辑系统用户'),
('用户删除', 'system:user:delete', '删除系统用户'),
('角色管理', 'system:role:manage', '角色管理'),
('参数配置', 'system:config:manage', '参数配置管理'),
('操作日志', 'system:log:view', '查看操作日志'),
('会员管理', 'member:manage', '会员管理'),
('商品管理', 'product:manage', '商品管理'),
('订单管理', 'order:manage', '订单管理'),
('退货审核', 'order:return:audit', '审核会员退货申请'),
('奖金管理', 'bonus:record:manage', '奖金记录管理'),
('奖金发放', 'bonus:record:grant', '奖金发放'),
('财务管理', 'finance:manage', '财务管理'),
('文件上传', 'oss:manage', '文件上传管理')
ON DUPLICATE KEY UPDATE id = id;

-- 超级管理员角色绑定所有权限
INSERT INTO ds_sys_role_permission (role_id, permission_id)
SELECT 1, id FROM ds_sys_permission
ON DUPLICATE KEY UPDATE role_id = role_id;