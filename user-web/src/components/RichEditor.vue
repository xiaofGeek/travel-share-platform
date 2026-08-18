<script setup>
import {ref,onMounted,watch} from 'vue'
import api from '../services/api.js'
import {ElMessage} from 'element-plus'
const props=defineProps({modelValue:String});const emit=defineEmits(['update:modelValue']);const editor=ref();const busy=ref(false)
onMounted(()=>{editor.value.innerHTML=props.modelValue||''});watch(()=>props.modelValue,v=>{if(editor.value&&editor.value.innerHTML!==v)editor.value.innerHTML=v||''})
const command=(name,value=null)=>{document.execCommand(name,false,value);editor.value.focus();sync()};const sync=()=>emit('update:modelValue',editor.value.innerHTML)
const upload=async e=>{const file=e.target.files?.[0];if(!file)return;busy.value=true;try{const form=new FormData();form.append('file',file);form.append('category','guide-content');const data=await api.post('/user/upload',form);command('insertImage',data.url);ElMessage.success('图片已插入正文')}catch(err){ElMessage.error(err.message)}finally{busy.value=false;e.target.value=''}}
</script>
<template><div class="rich-editor"><div class="editor-toolbar"><button type="button" @click="command('formatBlock','h2')">标题</button><button type="button" @click="command('bold')"><b>B</b></button><button type="button" @click="command('italic')"><i>I</i></button><button type="button" @click="command('insertUnorderedList')">列表</button><button type="button" @click="command('formatBlock','blockquote')">引用</button><label :class="{disabled:busy}">插入图片<input type="file" accept="image/*" hidden @change="upload"></label></div><div ref="editor" class="editor-canvas" contenteditable="true" data-placeholder="从一次真实的出发开始写起……" @input="sync"></div></div></template>

