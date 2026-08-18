import { createRouter,createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
const routes=[
  {path:'/',component:HomeView},
  {path:'/destinations',component:()=>import('../views/DestinationsView.vue')},{path:'/destination/:id',component:()=>import('../views/DestinationDetailView.vue')},
  {path:'/guides',component:()=>import('../views/GuidesView.vue')},{path:'/guide/:id',component:()=>import('../views/GuideDetailView.vue')},
  {path:'/routes',component:()=>import('../views/RoutesView.vue')},{path:'/route/:id',component:()=>import('../views/RouteDetailView.vue')},
  {path:'/topics',component:()=>import('../views/TopicsView.vue')},{path:'/topic/:id',component:()=>import('../views/TopicDetailView.vue')},
  {path:'/search',component:()=>import('../views/SearchView.vue')},{path:'/creator/:id',component:()=>import('../views/CreatorView.vue')},
  {path:'/login',component:()=>import('../views/LoginView.vue'),meta:{guest:true}},{path:'/register',component:()=>import('../views/RegisterView.vue'),meta:{guest:true}},
  {path:'/publish',component:()=>import('../views/PublishView.vue'),meta:{auth:true}},
  {path:'/user/guide/:id/edit',component:()=>import('../views/PublishView.vue'),meta:{auth:true}},
  {path:'/user/guide/:id',component:()=>import('../views/MyGuidePreviewView.vue'),meta:{auth:true}},
  {path:'/user/profile',component:()=>import('../views/UserCenterView.vue'),meta:{auth:true,section:'profile'}},
  {path:'/user/guides',component:()=>import('../views/UserCenterView.vue'),meta:{auth:true,section:'guides'}},{path:'/user/drafts',component:()=>import('../views/UserCenterView.vue'),meta:{auth:true,section:'drafts'}},
  {path:'/user/favorites',component:()=>import('../views/UserCenterView.vue'),meta:{auth:true,section:'favorites'}},{path:'/user/likes',component:()=>import('../views/UserCenterView.vue'),meta:{auth:true,section:'likes'}},
  {path:'/user/following',component:()=>import('../views/UserCenterView.vue'),meta:{auth:true,section:'following'}},{path:'/user/followers',component:()=>import('../views/UserCenterView.vue'),meta:{auth:true,section:'followers'}},
  {path:'/user/history',component:()=>import('../views/UserCenterView.vue'),meta:{auth:true,section:'history'}},{path:'/user/routes',component:()=>import('../views/UserCenterView.vue'),meta:{auth:true,section:'routes'}},
  {path:'/user/reports',component:()=>import('../views/UserCenterView.vue'),meta:{auth:true,section:'reports'}},{path:'/user/messages',component:()=>import('../views/UserCenterView.vue'),meta:{auth:true,section:'messages'}},{path:'/user/settings',component:()=>import('../views/UserCenterView.vue'),meta:{auth:true,section:'settings'}},
  {path:'/:pathMatch(.*)*',redirect:'/'}
]
const router=createRouter({history:createWebHistory(),routes,scrollBehavior:()=>({top:0})})
router.beforeEach(to=>{if(to.meta.auth&&!localStorage.getItem('travel_token'))return {path:'/login',query:{redirect:to.fullPath}}})
export default router
