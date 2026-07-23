<template>
  <div class="ds-page">
    <div class="ds-page__header">
      <div class="ds-page__header-title">
        <el-icon><Goods /></el-icon>
        <span>产品商城</span>
      </div>
      <div class="ds-page__header-actions">
        <el-badge :value="cartCount" :hidden="cartCount === 0">
          <el-button @click="router.push('/shop/cart')">
            <el-icon><ShoppingCart /></el-icon>购物车
          </el-button>
        </el-badge>
      </div>
    </div>

    <div class="ds-search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索产品名称"
        clearable
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      />
      <el-select
        v-model="searchCategory"
        placeholder="产品分类"
        clearable
        @change="handleSearch"
      >
        <el-option label="全部分类" value="" />
        <el-option
          v-for="cat in categoryList"
          :key="cat.id"
          :label="cat.name"
          :value="cat.id"
        />
      </el-select>
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>搜索
      </el-button>
    </div>

    <div v-loading="loading" class="ds-product-grid">
      <div
        v-for="product in products"
        :key="product.id"
        class="ds-product-card"
      >
        <div class="ds-product-card__image">
          <img v-if="product.mainImage" :src="product.mainImage" :alt="product.name" />
          <div v-else style="width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: #c0c4cc;">
            <el-icon :size="48"><Goods /></el-icon>
          </div>
          <el-tag
            v-if="product.isNew"
            class="ds-product-card__image-tag"
            type="danger"
            size="small"
          >
            新品
          </el-tag>
          <el-tag
            v-else-if="product.isRecommend"
            class="ds-product-card__image-tag"
            type="warning"
            size="small"
          >
            推荐
          </el-tag>
        </div>
        <div class="ds-product-card__body">
          <div class="ds-product-card__name">{{ product.name }}</div>
          <div class="ds-product-card__pv">
            <el-icon><Star /></el-icon>
            PV值：{{ product.pv || 0 }}
          </div>
          <div class="ds-product-card__footer">
            <div class="ds-product-card__price">
              <span class="ds-product-card__price-unit">¥</span>{{ product.memberPrice }}
            </div>
            <el-button
              class="ds-product-card__btn"
              type="primary"
              :disabled="product.stock <= 0"
              @click="handleAddToCart(product)"
            >
              <el-icon><ShoppingCart /></el-icon>
              {{ product.stock <= 0 ? '已售罄' : '加入购物车' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!loading && products.length === 0" class="ds-empty">
      <div class="ds-empty__icon">
        <el-icon><Goods /></el-icon>
      </div>
      <div class="ds-empty__text">暂无产品</div>
    </div>

    <el-pagination
      v-if="pagination.total > 0"
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.pageSize"
      :total="pagination.total"
      :page-sizes="[12, 24, 48]"
      layout="total, sizes, prev, pager, next, jumper"
      style="justify-content: center; margin-top: 24px;"
      @size-change="handleSizeChange"
      @current-change="handlePageChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Goods, ShoppingCart, Search, Star } from '@element-plus/icons-vue'

definePageMeta({
  layout: 'shop',
  middleware: 'auth',
})

const { get } = useApi()
const router = useRouter()

const loading = ref(false)
const products = ref<any[]>([])
const searchKeyword = ref('')
const searchCategory = ref('')
const cartCount = ref(0)
const categoryList = ref<any[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 12,
  total: 0,
})

async function fetchProducts() {
  loading.value = true
  try {
    const res: any = await get('/api/product/page', {
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchKeyword.value || undefined,
      categoryId: searchCategory.value || undefined,
    })
    const data = res.data || res
    products.value = data.records || data.list || []
    pagination.total = data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取产品列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchCategories() {
  try {
    const res: any = await get('/api/product/category/tree')
    categoryList.value = res.data || res || []
  } catch {
    // silently fail
  }
}

function handleSearch() {
  pagination.page = 1
  fetchProducts()
}

function handleSizeChange() {
  pagination.page = 1
  fetchProducts()
}

function handlePageChange() {
  fetchProducts()
}

function handleAddToCart(product: any) {
  try {
    const cart = JSON.parse(localStorage.getItem('shop_cart') || '[]')
    const existing = cart.find((item: any) => item.productId === product.id)
    if (existing) {
      existing.quantity += 1
    } else {
      cart.push({
        productId: product.id,
        name: product.name,
        image: product.mainImage,
        memberPrice: product.memberPrice,
        pv: product.pv,
        quantity: 1,
      })
    }
    localStorage.setItem('shop_cart', JSON.stringify(cart))
    updateCartCount()
    ElMessage.success(`已将「${product.name}」加入购物车`)
  } catch {
    ElMessage.error('操作失败')
  }
}

function updateCartCount() {
  try {
    const cart = JSON.parse(localStorage.getItem('shop_cart') || '[]')
    cartCount.value = cart.reduce((sum: number, item: any) => sum + item.quantity, 0)
  } catch {
    cartCount.value = 0
  }
}

onMounted(() => {
  fetchProducts()
  fetchCategories()
  updateCartCount()
})
</script>