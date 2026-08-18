<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  src: { type: String, default: '' },
  fallback: { type: String, default: '/uploads/demo/placeholders/placeholder-001.png' },
  alt: { type: String, default: '图片' }
})

const actualSrc = ref(props.src || props.fallback)
watch(() => props.src, value => { actualSrc.value = value || props.fallback })

const handleError = event => {
  if (actualSrc.value === props.fallback) return
  actualSrc.value = props.fallback
  event.target.dataset.fallbackApplied = 'true'
}
</script>

<template>
  <img :src="actualSrc" :alt="alt" loading="lazy" @error="handleError">
</template>
