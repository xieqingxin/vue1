import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Home from '../views/Home.vue'
import AddTask from '../views/AddTask.vue'
import TaskDetail from '../views/TaskDetail.vue'
import Profile from '../views/Profile.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/home' },
    { path: '/login', name: 'login', component: Login, meta: { guestOnly: true } },
    { path: '/register', name: 'register', component: Register, meta: { guestOnly: true } },
    { path: '/home', name: 'home', component: Home, meta: { requiresAuth: true } },
    { path: '/add/:type', name: 'addTask', component: AddTask, meta: { requiresAuth: true } },
    { path: '/task/:id', name: 'taskDetail', component: TaskDetail, meta: { requiresAuth: true } },
    { path: '/profile', name: 'profile', component: Profile, meta: { requiresAuth: true } }
  ]
})

router.beforeEach((to) => {
  const store = useUserStore()
  if (to.meta.requiresAuth && !store.isLoggedIn) {
    return { name: 'login' }
  }
  if (to.meta.guestOnly && store.isLoggedIn) {
    return { name: 'home' }
  }
})

export default router
