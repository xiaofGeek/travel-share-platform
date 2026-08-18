<script setup>defineProps({eyebrow:String,title:String,subtitle:String,more:String})</script>
<template><div class="section-heading"><div><span v-if="eyebrow" class="eyebrow">{{eyebrow}}</span><h2>{{title}}</h2><p v-if="subtitle">{{subtitle}}</p></div><router-link v-if="more" :to="more" class="text-link">查看全部 <span>→</span></router-link></div></template>

