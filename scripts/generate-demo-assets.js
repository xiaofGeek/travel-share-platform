/**
 * Generates deterministic, original local PNG artwork with Node built-ins only.
 * No network access, external fonts, image libraries, or copyrighted logos.
 */
import fs from 'node:fs'
import path from 'node:path'
import zlib from 'node:zlib'
import { fileURLToPath } from 'node:url'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(scriptDir, '..')
const sourceRoot = path.join(root, 'assets-source')
const uploadRoot = path.join(root, 'backend', 'uploads', 'demo')

const crcTable = new Uint32Array(256)
for (let n = 0; n < 256; n++) {
  let c = n
  for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1
  crcTable[n] = c >>> 0
}
function crc32(buffer) {
  let c = 0xffffffff
  for (const b of buffer) c = crcTable[(c ^ b) & 0xff] ^ (c >>> 8)
  return (c ^ 0xffffffff) >>> 0
}
function chunk(type, data) {
  const name = Buffer.from(type)
  const out = Buffer.alloc(data.length + 12)
  out.writeUInt32BE(data.length, 0); name.copy(out, 4); data.copy(out, 8)
  out.writeUInt32BE(crc32(Buffer.concat([name, data])), data.length + 8)
  return out
}
function encodePng(width, height, rgba) {
  const raw = Buffer.alloc((width * 4 + 1) * height)
  for (let y = 0; y < height; y++) {
    raw[y * (width * 4 + 1)] = 0
    rgba.copy(raw, y * (width * 4 + 1) + 1, y * width * 4, (y + 1) * width * 4)
  }
  const ihdr = Buffer.alloc(13)
  ihdr.writeUInt32BE(width, 0); ihdr.writeUInt32BE(height, 4)
  ihdr[8] = 8; ihdr[9] = 6
  return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw, { level: 9 })), chunk('IEND', Buffer.alloc(0))])
}

const clamp = n => Math.max(0, Math.min(255, Math.round(n)))
const hex = value => {
  const x = value.replace('#', '')
  return [parseInt(x.slice(0,2),16),parseInt(x.slice(2,4),16),parseInt(x.slice(4,6),16),255]
}
function canvas(width, height) {
  const data = Buffer.alloc(width * height * 4)
  const put = (x,y,c) => { if (x<0||y<0||x>=width||y>=height) return; const i=(Math.floor(y)*width+Math.floor(x))*4; data[i]=c[0];data[i+1]=c[1];data[i+2]=c[2];data[i+3]=c[3]??255 }
  const fill = c => { for(let i=0;i<data.length;i+=4){data[i]=c[0];data[i+1]=c[1];data[i+2]=c[2];data[i+3]=c[3]??255} }
  const rect = (x,y,w,h,c) => { for(let yy=Math.max(0,Math.floor(y));yy<Math.min(height,Math.ceil(y+h));yy++)for(let xx=Math.max(0,Math.floor(x));xx<Math.min(width,Math.ceil(x+w));xx++)put(xx,yy,c) }
  const circle = (cx,cy,r,c) => { for(let y=Math.floor(cy-r);y<=cy+r;y++)for(let x=Math.floor(cx-r);x<=cx+r;x++)if((x-cx)**2+(y-cy)**2<=r*r)put(x,y,c) }
  const polygon = (points,c) => { const minY=Math.max(0,Math.floor(Math.min(...points.map(p=>p[1]))));const maxY=Math.min(height-1,Math.ceil(Math.max(...points.map(p=>p[1]))));for(let y=minY;y<=maxY;y++){const xs=[];for(let i=0,j=points.length-1;i<points.length;j=i++){const a=points[i],b=points[j];if((a[1]>y)!=(b[1]>y))xs.push((b[0]-a[0])*(y-a[1])/(b[1]-a[1])+a[0])}xs.sort((a,b)=>a-b);for(let i=0;i<xs.length;i+=2)rect(xs[i],y,xs[i+1]-xs[i]+1,1,c)}}
  const gradient = (top,bottom) => { for(let y=0;y<height;y++){const t=y/(height-1),c=top.map((v,i)=>clamp(v+(bottom[i]-v)*t));rect(0,y,width,1,c)} }
  return { width,height,data,put,fill,rect,circle,polygon,gradient }
}

const palettes = [
  ['#bde9ff','#f7d7a8','#197fa6','#0e5f7f','#fff3ca'],
  ['#ffd6aa','#f59f72','#7b4333','#d25e48','#f7e6c7'],
  ['#cfeee8','#8fcab6','#2f7768','#275d63','#f2c66d'],
  ['#d5dcff','#8798d8','#314a72','#182c4c','#f3bf66'],
  ['#dff2c4','#a7d889','#41754c','#6e9d67','#f7d986'],
  ['#dbefff','#98b8d3','#e9f7ff','#52789d','#ffe8af']
]
function scene(width,height,seed,type=seed%6) {
  const c=canvas(width,height),p=palettes[(seed+type)%palettes.length].map(hex)
  c.gradient(p[0],p[1]); const horizon=Math.floor(height*(0.53+(seed%5)*0.015))
  c.circle(width*(0.16+(seed%7)*0.1),height*0.22,height*0.075,p[4])
  for(let i=0;i<4;i++){const x=((seed*97+i*271)%width),y=height*(0.12+i*0.07);c.circle(x,y,height*0.025,[255,255,255,120]);c.circle(x+height*.04,y,height*.035,[255,255,255,120]);c.circle(x+height*.08,y,height*.023,[255,255,255,120])}
  if(type===0){
    c.rect(0,horizon,width,height-horizon,p[2]);c.polygon([[0,horizon+height*.12],[width*.28,horizon-height*.09],[width*.52,horizon+height*.13]],p[3]);c.polygon([[width*.4,horizon+height*.08],[width*.75,horizon-height*.06],[width,horizon+height*.12]],p[3]);
    for(let i=0;i<8;i++)c.rect((i*149+seed*37)%width,horizon+20+i*17,width*.12,3,[255,255,255,120])
  } else if(type===1){
    c.rect(0,horizon,width,height-horizon,hex('#c9845d'));for(let i=0;i<7;i++){const x=i*width/7+(seed%3)*13,w=width/9,h=height*(.16+(i%3)*.05);c.rect(x,horizon-h,w,h,p[2]);c.polygon([[x-12,horizon-h],[x+w/2,horizon-h-height*.07],[x+w+12,horizon-h]],p[3]);for(let j=0;j<3;j++)c.rect(x+w*(.18+j*.25),horizon-h*.55,w*.1,h*.26,p[4])}c.rect(0,horizon+height*.1,width,height*.04,hex('#875341'))
  } else if(type===2){
    c.polygon([[0,horizon],[width*.22,height*.17],[width*.48,horizon]],p[3]);c.polygon([[width*.22,horizon],[width*.58,height*.12],[width*.82,horizon]],p[2]);c.polygon([[width*.55,horizon],[width*.82,height*.23],[width,horizon]],p[3]);c.polygon([[0,height],[width*.48,horizon],[width*.7,height]],hex('#9ad8cb'));c.rect(0,horizon,width,height-horizon,[105,176,155,255])
  } else if(type===3){
    c.rect(0,horizon,width,height-horizon,p[3]);for(let i=0;i<14;i++){const w=width*(.035+(i%4)*.013),h=height*(.15+((seed+i*3)%7)*.045),x=i*width/13-w/2;c.rect(x,horizon-h,w,h,i%2?p[2]:p[3]);for(let yy=horizon-h+14;yy<horizon-8;yy+=18)for(let xx=x+8;xx<x+w-6;xx+=15)c.rect(xx,yy,5,7,((i+yy)%3)?p[4]:hex('#9fd9e9'))}c.rect(0,horizon+height*.09,width,height*.05,hex('#e0b27c'))
  } else if(type===4){
    c.polygon([[0,horizon],[width*.28,height*.29],[width*.54,horizon]],p[2]);c.polygon([[width*.35,horizon],[width*.7,height*.25],[width,horizon]],p[3]);c.rect(0,horizon,width,height-horizon,hex('#76aa5e'));c.polygon([[width*.43,height],[width*.49,horizon],[width*.56,horizon],[width*.72,height]],hex('#d8c69a'));for(let i=0;i<4;i++){const x=(seed*79+i*251)%width;c.polygon([[x,horizon+height*.15],[x+height*.09,horizon+height*.03],[x+height*.18,horizon+height*.15]],p[1])}
  } else {
    c.polygon([[0,horizon],[width*.24,height*.16],[width*.5,horizon]],p[3]);c.polygon([[width*.25,horizon],[width*.58,height*.12],[width*.84,horizon]],hex('#f8fcff'));c.polygon([[width*.55,horizon],[width*.8,height*.2],[width,horizon]],p[3]);c.rect(0,horizon,width,height-horizon,hex('#eaf7ff'));for(let i=0;i<10;i++){const x=(i*179+seed*43)%width;c.rect(x,horizon-height*.07,height*.018,height*.17,p[3]);c.polygon([[x-height*.04,horizon],[x+height*.01,horizon-height*.19],[x+height*.06,horizon]],p[2])}
  }
  c.rect(0,height-5,width,5,[255,255,255,70]); return c.data
}
function avatar(size,seed){const c=canvas(size,size),p=palettes[seed%palettes.length].map(hex);c.gradient(p[0],p[1]);for(let i=0;i<5;i++)c.circle((seed*41+i*67)%size,(seed*19+i*47)%size,size*.12,[255,255,255,60]);c.circle(size*.5,size*.4,size*.19,p[4]);c.circle(size*.5,size*.43,size*.145,[242,191-(seed%4)*9,155-(seed%5)*7,255]);c.polygon([[size*.19,size],[size*.27,size*.69],[size*.5,size*.61],[size*.73,size*.69],[size*.81,size]],p[2]);c.circle(size*.45,size*.42,size*.014,p[3]);c.circle(size*.56,size*.42,size*.014,p[3]);return c.data}
function logo(size,seed){const c=canvas(size,size);c.gradient(hex('#e8f8fa'),hex('#92d5dd'));c.circle(size*.72,size*.24,size*.12,hex('#f6c76d'));c.polygon([[size*.08,size*.7],[size*.42,size*.24],[size*.69,size*.7]],hex('#246977'));c.polygon([[size*.34,size*.7],[size*.66,size*.35],[size*.94,size*.7]],hex('#3e8b87'));c.polygon([[0,size*.68],[size*.28,size*.6],[size*.58,size*.72],[size,size*.62],[size,size],[0,size]],hex('#147eaa'));for(let i=0;i<4;i++)c.rect(size*(.1+i*.23),size*(.78+i%2*.05),size*.16,size*.018,[255,255,255,160]);return c.data}

function write(relative,width,height,pixels){const src=path.join(sourceRoot,relative),dest=path.join(uploadRoot,relative);fs.mkdirSync(path.dirname(src),{recursive:true});fs.mkdirSync(path.dirname(dest),{recursive:true});const png=encodePng(width,height,pixels);fs.writeFileSync(src,png);fs.writeFileSync(dest,png)}
function batch(folder,prefix,count,width,height,offset=0){for(let i=1;i<=count;i++){const seed=i+offset;write(`${folder}/${prefix}-${String(i).padStart(3,'0')}.png`,width,height,scene(width,height,seed,seed%6))}}

batch('banners','banner',8,1400,640,0)
batch('destinations','destination',50,960,600,50)
batch('destinations/gallery','gallery',80,960,600,130)
batch('guides','guide',150,960,600,220)
batch('guides/content','content',80,1200,760,400)
batch('routes','route',60,960,600,500)
batch('topics','topic',20,960,600,580)
for(let i=1;i<=120;i++)write(`avatars/avatar-${String(i).padStart(3,'0')}.png`,256,256,avatar(256,i))
batch('scenic-spots','spot',30,960,600,650)
for(let i=1;i<=5;i++)write(`placeholders/placeholder-${String(i).padStart(3,'0')}.png`,960,600,scene(960,600,700+i,(i+2)%6))
write('logo.png',512,512,logo(512,1));write('favicon.png',64,64,logo(64,2))
console.log('Generated 605 original PNG files in assets-source and backend/uploads/demo.')

