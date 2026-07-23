<template>
  <div class="ds-page">
    <div class="ds-page__header">
      <div class="ds-page__header-title">
        <el-icon><DataBoard /></el-icon>
        <span>数据概览</span>
      </div>
      <div class="ds-page__header-actions">
        <el-button type="primary" :icon="Refresh" :loading="loading" @click="fetchOverview">
          刷新数据
        </el-button>
      </div>
    </div>

    <div class="ds-stat-grid">
      <div class="ds-stat-card">
        <div class="ds-stat-card__icon ds-stat-card__icon--blue">
          <el-icon><User /></el-icon>
        </div>
        <div class="ds-stat-card__info">
          <div class="ds-stat-card__title">会员总数</div>
          <div class="ds-stat-card__value">{{ overview.totalMembers.toLocaleString() }}</div>
          <div class="ds-stat-card__sub">今日新增 {{ overview.todayNewMembers }}</div>
        </div>
      </div>

      <div class="ds-stat-card">
        <div class="ds-stat-card__icon ds-stat-card__icon--green">
          <el-icon><UserFilled /></el-icon>
        </div>
        <div class="ds-stat-card__info">
          <div class="ds-stat-card__title">今日新增</div>
          <div class="ds-stat-card__value">{{ overview.todayNewMembers }}</div>
          <div class="ds-stat-card__sub">待处理订单 {{ overview.pendingOrders }}</div>
        </div>
      </div>

      <div class="ds-stat-card">
        <div class="ds-stat-card__icon ds-stat-card__icon--orange">
          <el-icon><List /></el-icon>
        </div>
        <div class="ds-stat-card__info">
          <div class="ds-stat-card__title">今日订单</div>
          <div class="ds-stat-card__value">{{ overview.todayOrders }}</div>
          <div class="ds-stat-card__sub">待处理 {{ overview.pendingOrders }} 笔</div>
        </div>
      </div>

      <div class="ds-stat-card">
        <div class="ds-stat-card__icon ds-stat-card__icon--purple">
          <el-icon><Money /></el-icon>
        </div>
        <div class="ds-stat-card__info">
          <div class="ds-stat-card__title">今日销售额</div>
          <div class="ds-stat-card__value">¥{{ overview.todaySales.toLocaleString() }}</div>
          <div class="ds-stat-card__sub">本月累计 ¥{{ overview.monthSales.toLocaleString() }}</div>
        </div>
      </div>
    </div>

    <div class="ds-chart-row">
      <el-card shadow="hover">
        <template #header>
          <div class="ds-page__header-title">
            <span>销售趋势</span>
          </div>
        </template>
        <div class="ds-chart-box">
          <v-chart v-if="salesChartOption" :option="salesChartOption" autoresize />
          <div v-else class="ds-empty">
            <div class="ds-empty__icon">
              <el-icon><DataBoard /></el-icon>
            </div>
            <div class="ds-empty__text">暂无数据</div>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover">
        <template #header>
          <div class="ds-page__header-title">
            <span>会员增长</span>
          </div>
        </template>
        <div class="ds-chart-box">
          <v-chart v-if="memberChartOption" :option="memberChartOption" autoresize />
          <div v-else class="ds-empty">
            <div class="ds-empty__icon">
              <el-icon><UserFilled /></el-icon>
            </div>
            <div class="ds-empty__text">暂无数据</div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { DataBoard, User, UserFilled, List, Money, Refresh } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent])

definePageMeta({
  layout: 'admin',
  middleware: ['auth', 'permission'],
  permissions: ['system:manage', 'report:view'],
})

const { get } = useApi()

const loading = ref(false)

const overview = reactive({
  totalMembers: 0,
  todayNewMembers: 0,
  todayOrders: 0,
  pendingOrders: 0,
  todaySales: 0,
  monthSales: 0,
  totalBonus: 0,
  monthBonus: 0,
})

const salesChartOption = ref<any>(null)
const memberChartOption = ref<any>(null)

async function fetchOverview() {
  loading.value = true
  try {
    const res: any = await get('/api/report/dashboard/overview')
    const data = res.data || res

    overview.totalMembers = data.totalMembers || 0
    overview.todayNewMembers = data.todayNewMembers || 0
    overview.todayOrders = data.todayOrders || 0
    overview.pendingOrders = data.pendingOrders || 0
    overview.todaySales = data.todaySales || 0
    overview.monthSales = data.monthSales || 0
    overview.totalBonus = data.totalBonus || 0
    overview.monthBonus = data.monthBonus || 0

    if (data.salesTrend && data.salesTrend.length > 0) {
      salesChartOption.value = {
        tooltip: { trigger: 'axis' },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          data: data.salesTrend.map((item: any) => item.date),
          boundaryGap: false,
        },
        yAxis: { type: 'value' },
        series: [
          {
            name: '销售额',
            type: 'line',
            data: data.salesTrend.map((item: any) => item.amount),
            smooth: true,
            areaStyle: {
              color: {
                type: 'linear',
                x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: 'rgba(64,158,255,0.3)' },
                  { offset: 1, color: 'rgba(64,158,255,0.05)' },
                ],
              },
            },
            itemStyle: { color: '#409eff' },
          },
        ],
      }
    } else {
      salesChartOption.value = null
    }

    if (data.memberTrend && data.memberTrend.length > 0) {
      memberChartOption.value = {
        tooltip: { trigger: 'axis' },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          data: data.memberTrend.map((item: any) => item.date),
        },
        yAxis: { type: 'value' },
        series: [
          {
            name: '新增会员',
            type: 'bar',
            data: data.memberTrend.map((item: any) => item.count),
            itemStyle: {
              color: {
                type: 'linear',
                x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: '#67c23a' },
                  { offset: 1, color: '#b3e19d' },
                ],
              },
            },
            barMaxWidth: 30,
          },
        ],
      }
    } else {
      memberChartOption.value = null
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchOverview()
})
</script>