<script setup>defineProps({image:String,eyebrow:String,title:String,subtitle:String})</script>
<template><section class="page-hero" :style="{backgroundImage:`linear-gradient(90deg,rgba(8,37,46,.78),rgba(8,37,46,.15)),url(${image||'/uploads/demo/banners/banner-001.png'})`}"><div class="container"><span>{{eyebrow}}</span><h1>{{title}}</h1><p>{{subtitle}}</p><slot/></div></section></template>

