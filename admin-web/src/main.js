import{createApp}from'vue';import ElementPlus from'element-plus';import'element-plus/dist/index.css';import{createPinia}from'pinia';import App from'./App.vue';import router from'./router/index.js';import'./assets/main.css';
const imageFallback='/uploads/demo/placeholders/placeholder-001.png';
document.addEventListener('error',event=>{const target=event.target;if(target instanceof HTMLImageElement&&!target.dataset.fallbackApplied){target.dataset.fallbackApplied='true';target.src=imageFallback}},true);
createApp(App).use(createPinia()).use(router).use(ElementPlus).mount('#app')
