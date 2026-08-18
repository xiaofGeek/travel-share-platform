<script setup>defineProps({item:{type:Object,required:true},large:Boolean})</script>
<template><router-link :to="`/destination/${item.id}`" class="destination-card" :class="{large}"><img :src="item.coverImage" :alt="item.name" loading="lazy" @error="$event.target.src='/uploads/demo/placeholders/placeholder-002.png'"><div class="image-shade"></div><div class="destination-copy"><span>{{item.nameEn}}</span><h3>{{item.name}}</h3><p>{{item.summary}}</p><div><b>{{item.guideCount}}</b> 篇攻略 · {{Number(item.favoriteCount||0).toLocaleString()}} 收藏</div></div></router-link></template>

