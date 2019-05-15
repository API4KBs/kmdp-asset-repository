/*
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
