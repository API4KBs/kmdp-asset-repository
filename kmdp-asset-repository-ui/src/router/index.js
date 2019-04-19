import Vue from 'vue';
import Router from 'vue-router';
import HelloWorld from '@/components/HomePage';
import Asset from '@/components/Asset';
import VueHotkey from 'v-hotkey';
import VueHighlightJS from 'vue-highlightjs';

Vue.use(Router);
Vue.use(VueHotkey);
Vue.use(VueHighlightJS);

export default new Router({
  routes: [
    {
      path: '/',
      name: 'HomePage',
      component: HelloWorld
    },
    {
      path: '/assets/:id',
      name: 'Asset',
      component: Asset
    }
  ]
});
