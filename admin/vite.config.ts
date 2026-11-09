import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
export default defineConfig({plugins:[vue()],server:{host:'127.0.0.1',port:5174,proxy:{'/monitor':{target:'http://127.0.0.1:8088'},'/api':{target:'http://127.0.0.1:8088'},'/auth':{target:'http://127.0.0.1:8088'},'/system':{target:'http://127.0.0.1:8088'}}}})
