/******/
(function (modules) { // webpackBootstrap
  /******/ 	// install a JSONP callback for chunk loading
  /******/
  function webpackJsonpCallback(data) {
    /******/
    var chunkIds = data[0];
    /******/
    var moreModules = data[1];
    /******/
    var executeModules = data[2];
    /******/
    /******/ 		// add "moreModules" to the modules object,
    /******/ 		// then flag all "chunkIds" as loaded and fire callback
    /******/
    var moduleId, chunkId, i = 0, resolves = [];
    /******/
    for (; i < chunkIds.length; i++) {
      /******/
      chunkId = chunkIds[i];
      /******/
      if (installedChunks[chunkId]) {
        /******/
        resolves.push(installedChunks[chunkId][0]);
        /******/
      }
      /******/
      installedChunks[chunkId] = 0;
      /******/
    }
    /******/
    for (moduleId in moreModules) {
      /******/
      if (Object.prototype.hasOwnProperty.call(moreModules, moduleId)) {
        /******/
        modules[moduleId] = moreModules[moduleId];
        /******/
      }
      /******/
    }
    /******/
    if (parentJsonpFunction) {
      parentJsonpFunction(data);
    }
    /******/
    /******/
    while (resolves.length) {
      /******/
      resolves.shift()();
      /******/
    }
    /******/
    /******/ 		// add entry modules from loaded chunk to deferred list
    /******/
    deferredModules.push.apply(deferredModules, executeModules || []);
    /******/
    /******/ 		// run deferred modules when all chunks ready
    /******/
    return checkDeferredModules();
    /******/
  };

  /******/
  function checkDeferredModules() {
    /******/
    var result;
    /******/
    for (var i = 0; i < deferredModules.length; i++) {
      /******/
      var deferredModule = deferredModules[i];
      /******/
      var fulfilled = true;
      /******/
      for (var j = 1; j < deferredModule.length; j++) {
        /******/
        var depId = deferredModule[j];
        /******/
        if (installedChunks[depId] !== 0) {
          fulfilled = false;
        }
        /******/
      }
      /******/
      if (fulfilled) {
        /******/
        deferredModules.splice(i--, 1);
        /******/
        result = __webpack_require__(__webpack_require__.s = deferredModule[0]);
        /******/
      }
      /******/
    }
    /******/
    return result;
    /******/
  }

  /******/
  /******/ 	// The module cache
  /******/
  var installedModules = {};
  /******/
  /******/ 	// object to store loaded and loading chunks
  /******/ 	// undefined = chunk not loaded, null = chunk preloaded/prefetched
  /******/ 	// Promise = chunk loading, 0 = chunk loaded
  /******/
  var installedChunks = {
    /******/    "app": 0
    /******/
  };
  /******/
  /******/
  var deferredModules = [];
  /******/
  /******/ 	// The require function
  /******/
  function __webpack_require__(moduleId) {
    /******/
    /******/ 		// Check if module is in cache
    /******/
    if (installedModules[moduleId]) {
      /******/
      return installedModules[moduleId].exports;
      /******/
    }
    /******/ 		// Create a new module (and put it into the cache)
    /******/
    var module = installedModules[moduleId] = {
      /******/      i: moduleId,
      /******/      l: false,
      /******/      exports: {}
      /******/
    };
    /******/
    /******/ 		// Execute the module function
    /******/
    modules[moduleId].call(module.exports, module, module.exports,
        __webpack_require__);
    /******/
    /******/ 		// Flag the module as loaded
    /******/
    module.l = true;
    /******/
    /******/ 		// Return the exports of the module
    /******/
    return module.exports;
    /******/
  }

  /******/
  /******/
  /******/ 	// expose the modules object (__webpack_modules__)
  /******/
  __webpack_require__.m = modules;
  /******/
  /******/ 	// expose the module cache
  /******/
  __webpack_require__.c = installedModules;
  /******/
  /******/ 	// define getter function for harmony exports
  /******/
  __webpack_require__.d = function (exports, name, getter) {
    /******/
    if (!__webpack_require__.o(exports, name)) {
      /******/
      Object.defineProperty(exports, name, {enumerable: true, get: getter});
      /******/
    }
    /******/
  };
  /******/
  /******/ 	// define __esModule on exports
  /******/
  __webpack_require__.r = function (exports) {
    /******/
    if (typeof Symbol !== 'undefined' && Symbol.toStringTag) {
      /******/
      Object.defineProperty(exports, Symbol.toStringTag, {value: 'Module'});
      /******/
    }
    /******/
    Object.defineProperty(exports, '__esModule', {value: true});
    /******/
  };
  /******/
  /******/ 	// create a fake namespace object
  /******/ 	// mode & 1: value is a module id, require it
  /******/ 	// mode & 2: merge all properties of value into the ns
  /******/ 	// mode & 4: return value when already ns object
  /******/ 	// mode & 8|1: behave like require
  /******/
  __webpack_require__.t = function (value, mode) {
    /******/
    if (mode & 1) {
      value = __webpack_require__(value);
    }
    /******/
    if (mode & 8) {
      return value;
    }
    /******/
    if ((mode & 4) && typeof value === 'object' && value
        && value.__esModule) {
      return value;
    }
    /******/
    var ns = Object.create(null);
    /******/
    __webpack_require__.r(ns);
    /******/
    Object.defineProperty(ns, 'default', {enumerable: true, value: value});
    /******/
    if (mode & 2 && typeof value != 'string') {
      for (var key in
          value) {
        __webpack_require__.d(ns, key, function (key) {
          return value[key];
        }.bind(null, key));
      }
    }
    /******/
    return ns;
    /******/
  };
  /******/
  /******/ 	// getDefaultExport function for compatibility with non-harmony modules
  /******/
  __webpack_require__.n = function (module) {
    /******/
    var getter = module && module.__esModule ?
        /******/      function getDefault() {
          return module['default'];
        } :
        /******/      function getModuleExports() {
          return module;
        };
    /******/
    __webpack_require__.d(getter, 'a', getter);
    /******/
    return getter;
    /******/
  };
  /******/
  /******/ 	// Object.prototype.hasOwnProperty.call
  /******/
  __webpack_require__.o = function (object, property) {
    return Object.prototype.hasOwnProperty.call(object, property);
  };
  /******/
  /******/ 	// __webpack_public_path__
  /******/
  __webpack_require__.p = "/";
  /******/
  /******/
  var jsonpArray = window["webpackJsonp"] = window["webpackJsonp"] || [];
  /******/
  var oldJsonpFunction = jsonpArray.push.bind(jsonpArray);
  /******/
  jsonpArray.push = webpackJsonpCallback;
  /******/
  jsonpArray = jsonpArray.slice();
  /******/
  for (var i = 0; i < jsonpArray.length; i++) {
    webpackJsonpCallback(
        jsonpArray[i]);
  }
  /******/
  var parentJsonpFunction = oldJsonpFunction;
  /******/
  /******/
  /******/ 	// add entry module to deferred list
  /******/
  deferredModules.push(["./src/main.js", "vendors~app"]);
  /******/ 	// run deferred modules when ready
  /******/
  return checkDeferredModules();
  /******/
})
/************************************************************************/
/******/({

  /***/
  "./node_modules/babel-loader/lib/index.js!./node_modules/vue-loader/lib/selector.js?type=script&index=0!./src/App.vue":
  /*!*******************************************************************************************************************!*\
    !*** ./node_modules/babel-loader/lib!./node_modules/vue-loader/lib/selector.js?type=script&index=0!./src/App.vue ***!
    \*******************************************************************************************************************/
  /*! exports provided: default */
  /***/ (function (module, __webpack_exports__, __webpack_require__) {

    "use strict";
    __webpack_require__.r(__webpack_exports__);
    /* harmony import */
    var mdbvue__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! mdbvue */
        "./node_modules/mdbvue/src/index.js");
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//

    /* harmony default export */
    __webpack_exports__["default"] = ({
      name: 'app',
      components: {
        Navbar: mdbvue__WEBPACK_IMPORTED_MODULE_0__["Navbar"],
        NavbarItem: mdbvue__WEBPACK_IMPORTED_MODULE_0__["NavbarItem"],
        NavbarNav: mdbvue__WEBPACK_IMPORTED_MODULE_0__["NavbarNav"],
        NavbarCollapse: mdbvue__WEBPACK_IMPORTED_MODULE_0__["NavbarCollapse"],
        mdbNavbarBrand: mdbvue__WEBPACK_IMPORTED_MODULE_0__["mdbNavbarBrand"],
        Footer: mdbvue__WEBPACK_IMPORTED_MODULE_0__["Footer"]
      }
    });

    /***/
  }),

  /***/
  "./node_modules/babel-loader/lib/index.js!./node_modules/vue-loader/lib/selector.js?type=script&index=0!./src/components/Asset.vue":
  /*!********************************************************************************************************************************!*\
    !*** ./node_modules/babel-loader/lib!./node_modules/vue-loader/lib/selector.js?type=script&index=0!./src/components/Asset.vue ***!
    \********************************************************************************************************************************/
  /*! exports provided: default */
  /***/ (function (module, __webpack_exports__, __webpack_require__) {

    "use strict";
    __webpack_require__.r(__webpack_exports__);
    /* harmony import */
    var mdbvue__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! mdbvue */
        "./node_modules/mdbvue/src/index.js");
    /* harmony import */
    var axios__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! axios */
        "./node_modules/axios/index.js");
    /* harmony import */
    var axios__WEBPACK_IMPORTED_MODULE_1___default = /*#__PURE__*/__webpack_require__.n(
        axios__WEBPACK_IMPORTED_MODULE_1__);
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//

    /* harmony default export */
    __webpack_exports__["default"] = ({
      name: 'Asset',
      components: {
        Container: mdbvue__WEBPACK_IMPORTED_MODULE_0__["Container"],
        Column: mdbvue__WEBPACK_IMPORTED_MODULE_0__["Column"],
        Row: mdbvue__WEBPACK_IMPORTED_MODULE_0__["Row"],
        Fa: mdbvue__WEBPACK_IMPORTED_MODULE_0__["Fa"],
        Navbar: mdbvue__WEBPACK_IMPORTED_MODULE_0__["Navbar"],
        NavbarItem: mdbvue__WEBPACK_IMPORTED_MODULE_0__["NavbarItem"],
        NavbarNav: mdbvue__WEBPACK_IMPORTED_MODULE_0__["NavbarNav"],
        NavbarCollapse: mdbvue__WEBPACK_IMPORTED_MODULE_0__["NavbarCollapse"],
        Btn: mdbvue__WEBPACK_IMPORTED_MODULE_0__["Btn"],
        EdgeHeader: mdbvue__WEBPACK_IMPORTED_MODULE_0__["EdgeHeader"],
        CardBody: mdbvue__WEBPACK_IMPORTED_MODULE_0__["CardBody"]
      },
      created: function created() {
        this.getAsset();
      },
      data: function data() {
        return {
          asset: null,
          carrier: null
        };
      },

      methods: {
        decode: function decode(s) {
          return atob(s);
        },
        getAsset: function getAsset() {
          var _this = this;

          axios__WEBPACK_IMPORTED_MODULE_1___default.a.get(
              '/cat/assets/' + this.$route.params.id).then(function (asset) {
            _this.asset = asset.data;

            axios__WEBPACK_IMPORTED_MODULE_1___default.a.get(
                '/cat/assets/' + _this.asset.resourceId.tag + '/versions/'
                + _this.asset.resourceId.version + '/carrier').then(
                function (carrier) {
                  _this.carrier = carrier.data;
                }).catch(function (e) {
              throw e;
            });
          }).catch(function (e) {
            throw e;
          });
        }
      }
    });

    /***/
  }),

  /***/
  "./node_modules/babel-loader/lib/index.js!./node_modules/vue-loader/lib/selector.js?type=script&index=0!./src/components/HomePage.vue":
  /*!***********************************************************************************************************************************!*\
    !*** ./node_modules/babel-loader/lib!./node_modules/vue-loader/lib/selector.js?type=script&index=0!./src/components/HomePage.vue ***!
    \***********************************************************************************************************************************/
  /*! exports provided: default */
  /***/ (function (module, __webpack_exports__, __webpack_require__) {

    "use strict";
    __webpack_require__.r(__webpack_exports__);
    /* harmony import */
    var mdbvue__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! mdbvue */
        "./node_modules/mdbvue/src/index.js");
    /* harmony import */
    var axios__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! axios */
        "./node_modules/axios/index.js");
    /* harmony import */
    var axios__WEBPACK_IMPORTED_MODULE_1___default = /*#__PURE__*/__webpack_require__.n(
        axios__WEBPACK_IMPORTED_MODULE_1__);
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//

    /* harmony default export */
    __webpack_exports__["default"] = ({
      name: 'HomePage',
      components: {
        Container: mdbvue__WEBPACK_IMPORTED_MODULE_0__["Container"],
        Column: mdbvue__WEBPACK_IMPORTED_MODULE_0__["Column"],
        Row: mdbvue__WEBPACK_IMPORTED_MODULE_0__["Row"],
        Fa: mdbvue__WEBPACK_IMPORTED_MODULE_0__["Fa"],
        Navbar: mdbvue__WEBPACK_IMPORTED_MODULE_0__["Navbar"],
        NavbarItem: mdbvue__WEBPACK_IMPORTED_MODULE_0__["NavbarItem"],
        NavbarNav: mdbvue__WEBPACK_IMPORTED_MODULE_0__["NavbarNav"],
        NavbarCollapse: mdbvue__WEBPACK_IMPORTED_MODULE_0__["NavbarCollapse"],
        Btn: mdbvue__WEBPACK_IMPORTED_MODULE_0__["Btn"],
        EdgeHeader: mdbvue__WEBPACK_IMPORTED_MODULE_0__["EdgeHeader"],
        CardBody: mdbvue__WEBPACK_IMPORTED_MODULE_0__["CardBody"]
      },
      created: function created() {
        this.getAssets();
      },
      data: function data() {
        return {
          assets: []
        };
      },

      methods: {
        getAssets: function getAssets() {
          var _this = this;

          axios__WEBPACK_IMPORTED_MODULE_1___default.a.get('/cat/assets').then(
              function (assets) {
                return _this.assets = assets.data;
              }).catch(function (e) {
            throw e;
          });
        }
      }
    });

    /***/
  }),

  /***/
  "./node_modules/extract-text-webpack-plugin/dist/loader.js?{\"omit\":1,\"remove\":true}!./node_modules/vue-style-loader/index.js!./node_modules/css-loader/index.js?{\"sourceMap\":true}!./node_modules/vue-loader/lib/style-compiler/index.js?{\"optionsId\":\"0\",\"vue\":true,\"id\":\"data-v-2715fe5c\",\"scoped\":true,\"sourceMap\":false}!./node_modules/vue-loader/lib/selector.js?type=styles&index=0!./src/components/HomePage.vue":
  /*!****************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************!*\
    !*** ./node_modules/extract-text-webpack-plugin/dist/loader.js?{"omit":1,"remove":true}!./node_modules/vue-style-loader!./node_modules/css-loader?{"sourceMap":true}!./node_modules/vue-loader/lib/style-compiler?{"optionsId":"0","vue":true,"id":"data-v-2715fe5c","scoped":true,"sourceMap":false}!./node_modules/vue-loader/lib/selector.js?type=styles&index=0!./src/components/HomePage.vue ***!
    \****************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************/
  /*! no static exports found */
  /***/ (function (module, exports) {

// removed by extract-text-webpack-plugin

    /***/
  }),

  /***/
  "./node_modules/extract-text-webpack-plugin/dist/loader.js?{\"omit\":1,\"remove\":true}!./node_modules/vue-style-loader/index.js!./node_modules/css-loader/index.js?{\"sourceMap\":true}!./node_modules/vue-loader/lib/style-compiler/index.js?{\"optionsId\":\"0\",\"vue\":true,\"id\":\"data-v-6be0f6b0\",\"scoped\":true,\"sourceMap\":false}!./node_modules/vue-loader/lib/selector.js?type=styles&index=0!./src/components/Asset.vue":
  /*!*************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************!*\
    !*** ./node_modules/extract-text-webpack-plugin/dist/loader.js?{"omit":1,"remove":true}!./node_modules/vue-style-loader!./node_modules/css-loader?{"sourceMap":true}!./node_modules/vue-loader/lib/style-compiler?{"optionsId":"0","vue":true,"id":"data-v-6be0f6b0","scoped":true,"sourceMap":false}!./node_modules/vue-loader/lib/selector.js?type=styles&index=0!./src/components/Asset.vue ***!
    \*************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************/
  /*! no static exports found */
  /***/ (function (module, exports) {

// removed by extract-text-webpack-plugin

    /***/
  }),

  /***/
  "./node_modules/extract-text-webpack-plugin/dist/loader.js?{\"omit\":1,\"remove\":true}!./node_modules/vue-style-loader/index.js!./node_modules/css-loader/index.js?{\"sourceMap\":true}!./node_modules/vue-loader/lib/style-compiler/index.js?{\"optionsId\":\"0\",\"vue\":true,\"scoped\":false,\"sourceMap\":false}!./node_modules/vue-loader/lib/selector.js?type=styles&index=0!./src/App.vue":
  /*!**************************************************************************************************************************************************************************************************************************************************************************************************************************************************************!*\
    !*** ./node_modules/extract-text-webpack-plugin/dist/loader.js?{"omit":1,"remove":true}!./node_modules/vue-style-loader!./node_modules/css-loader?{"sourceMap":true}!./node_modules/vue-loader/lib/style-compiler?{"optionsId":"0","vue":true,"scoped":false,"sourceMap":false}!./node_modules/vue-loader/lib/selector.js?type=styles&index=0!./src/App.vue ***!
    \**************************************************************************************************************************************************************************************************************************************************************************************************************************************************************/
  /*! no static exports found */
  /***/ (function (module, exports) {

// removed by extract-text-webpack-plugin

    /***/
  }),

  /***/
  "./node_modules/moment/locale sync recursive ^\\.\\/.*$":
  /*!**************************************************!*\
    !*** ./node_modules/moment/locale sync ^\.\/.*$ ***!
    \**************************************************/
  /*! no static exports found */
  /***/ (function (module, exports, __webpack_require__) {

    var map = {
      "./af": "./node_modules/moment/locale/af.js",
      "./af.js": "./node_modules/moment/locale/af.js",
      "./ar": "./node_modules/moment/locale/ar.js",
      "./ar-dz": "./node_modules/moment/locale/ar-dz.js",
      "./ar-dz.js": "./node_modules/moment/locale/ar-dz.js",
      "./ar-kw": "./node_modules/moment/locale/ar-kw.js",
      "./ar-kw.js": "./node_modules/moment/locale/ar-kw.js",
      "./ar-ly": "./node_modules/moment/locale/ar-ly.js",
      "./ar-ly.js": "./node_modules/moment/locale/ar-ly.js",
      "./ar-ma": "./node_modules/moment/locale/ar-ma.js",
      "./ar-ma.js": "./node_modules/moment/locale/ar-ma.js",
      "./ar-sa": "./node_modules/moment/locale/ar-sa.js",
      "./ar-sa.js": "./node_modules/moment/locale/ar-sa.js",
      "./ar-tn": "./node_modules/moment/locale/ar-tn.js",
      "./ar-tn.js": "./node_modules/moment/locale/ar-tn.js",
      "./ar.js": "./node_modules/moment/locale/ar.js",
      "./az": "./node_modules/moment/locale/az.js",
      "./az.js": "./node_modules/moment/locale/az.js",
      "./be": "./node_modules/moment/locale/be.js",
      "./be.js": "./node_modules/moment/locale/be.js",
      "./bg": "./node_modules/moment/locale/bg.js",
      "./bg.js": "./node_modules/moment/locale/bg.js",
      "./bm": "./node_modules/moment/locale/bm.js",
      "./bm.js": "./node_modules/moment/locale/bm.js",
      "./bn": "./node_modules/moment/locale/bn.js",
      "./bn.js": "./node_modules/moment/locale/bn.js",
      "./bo": "./node_modules/moment/locale/bo.js",
      "./bo.js": "./node_modules/moment/locale/bo.js",
      "./br": "./node_modules/moment/locale/br.js",
      "./br.js": "./node_modules/moment/locale/br.js",
      "./bs": "./node_modules/moment/locale/bs.js",
      "./bs.js": "./node_modules/moment/locale/bs.js",
      "./ca": "./node_modules/moment/locale/ca.js",
      "./ca.js": "./node_modules/moment/locale/ca.js",
      "./cs": "./node_modules/moment/locale/cs.js",
      "./cs.js": "./node_modules/moment/locale/cs.js",
      "./cv": "./node_modules/moment/locale/cv.js",
      "./cv.js": "./node_modules/moment/locale/cv.js",
      "./cy": "./node_modules/moment/locale/cy.js",
      "./cy.js": "./node_modules/moment/locale/cy.js",
      "./da": "./node_modules/moment/locale/da.js",
      "./da.js": "./node_modules/moment/locale/da.js",
      "./de": "./node_modules/moment/locale/de.js",
      "./de-at": "./node_modules/moment/locale/de-at.js",
      "./de-at.js": "./node_modules/moment/locale/de-at.js",
      "./de-ch": "./node_modules/moment/locale/de-ch.js",
      "./de-ch.js": "./node_modules/moment/locale/de-ch.js",
      "./de.js": "./node_modules/moment/locale/de.js",
      "./dv": "./node_modules/moment/locale/dv.js",
      "./dv.js": "./node_modules/moment/locale/dv.js",
      "./el": "./node_modules/moment/locale/el.js",
      "./el.js": "./node_modules/moment/locale/el.js",
      "./en-au": "./node_modules/moment/locale/en-au.js",
      "./en-au.js": "./node_modules/moment/locale/en-au.js",
      "./en-ca": "./node_modules/moment/locale/en-ca.js",
      "./en-ca.js": "./node_modules/moment/locale/en-ca.js",
      "./en-gb": "./node_modules/moment/locale/en-gb.js",
      "./en-gb.js": "./node_modules/moment/locale/en-gb.js",
      "./en-ie": "./node_modules/moment/locale/en-ie.js",
      "./en-ie.js": "./node_modules/moment/locale/en-ie.js",
      "./en-il": "./node_modules/moment/locale/en-il.js",
      "./en-il.js": "./node_modules/moment/locale/en-il.js",
      "./en-nz": "./node_modules/moment/locale/en-nz.js",
      "./en-nz.js": "./node_modules/moment/locale/en-nz.js",
      "./eo": "./node_modules/moment/locale/eo.js",
      "./eo.js": "./node_modules/moment/locale/eo.js",
      "./es": "./node_modules/moment/locale/es.js",
      "./es-do": "./node_modules/moment/locale/es-do.js",
      "./es-do.js": "./node_modules/moment/locale/es-do.js",
      "./es-us": "./node_modules/moment/locale/es-us.js",
      "./es-us.js": "./node_modules/moment/locale/es-us.js",
      "./es.js": "./node_modules/moment/locale/es.js",
      "./et": "./node_modules/moment/locale/et.js",
      "./et.js": "./node_modules/moment/locale/et.js",
      "./eu": "./node_modules/moment/locale/eu.js",
      "./eu.js": "./node_modules/moment/locale/eu.js",
      "./fa": "./node_modules/moment/locale/fa.js",
      "./fa.js": "./node_modules/moment/locale/fa.js",
      "./fi": "./node_modules/moment/locale/fi.js",
      "./fi.js": "./node_modules/moment/locale/fi.js",
      "./fo": "./node_modules/moment/locale/fo.js",
      "./fo.js": "./node_modules/moment/locale/fo.js",
      "./fr": "./node_modules/moment/locale/fr.js",
      "./fr-ca": "./node_modules/moment/locale/fr-ca.js",
      "./fr-ca.js": "./node_modules/moment/locale/fr-ca.js",
      "./fr-ch": "./node_modules/moment/locale/fr-ch.js",
      "./fr-ch.js": "./node_modules/moment/locale/fr-ch.js",
      "./fr.js": "./node_modules/moment/locale/fr.js",
      "./fy": "./node_modules/moment/locale/fy.js",
      "./fy.js": "./node_modules/moment/locale/fy.js",
      "./gd": "./node_modules/moment/locale/gd.js",
      "./gd.js": "./node_modules/moment/locale/gd.js",
      "./gl": "./node_modules/moment/locale/gl.js",
      "./gl.js": "./node_modules/moment/locale/gl.js",
      "./gom-latn": "./node_modules/moment/locale/gom-latn.js",
      "./gom-latn.js": "./node_modules/moment/locale/gom-latn.js",
      "./gu": "./node_modules/moment/locale/gu.js",
      "./gu.js": "./node_modules/moment/locale/gu.js",
      "./he": "./node_modules/moment/locale/he.js",
      "./he.js": "./node_modules/moment/locale/he.js",
      "./hi": "./node_modules/moment/locale/hi.js",
      "./hi.js": "./node_modules/moment/locale/hi.js",
      "./hr": "./node_modules/moment/locale/hr.js",
      "./hr.js": "./node_modules/moment/locale/hr.js",
      "./hu": "./node_modules/moment/locale/hu.js",
      "./hu.js": "./node_modules/moment/locale/hu.js",
      "./hy-am": "./node_modules/moment/locale/hy-am.js",
      "./hy-am.js": "./node_modules/moment/locale/hy-am.js",
      "./id": "./node_modules/moment/locale/id.js",
      "./id.js": "./node_modules/moment/locale/id.js",
      "./is": "./node_modules/moment/locale/is.js",
      "./is.js": "./node_modules/moment/locale/is.js",
      "./it": "./node_modules/moment/locale/it.js",
      "./it.js": "./node_modules/moment/locale/it.js",
      "./ja": "./node_modules/moment/locale/ja.js",
      "./ja.js": "./node_modules/moment/locale/ja.js",
      "./jv": "./node_modules/moment/locale/jv.js",
      "./jv.js": "./node_modules/moment/locale/jv.js",
      "./ka": "./node_modules/moment/locale/ka.js",
      "./ka.js": "./node_modules/moment/locale/ka.js",
      "./kk": "./node_modules/moment/locale/kk.js",
      "./kk.js": "./node_modules/moment/locale/kk.js",
      "./km": "./node_modules/moment/locale/km.js",
      "./km.js": "./node_modules/moment/locale/km.js",
      "./kn": "./node_modules/moment/locale/kn.js",
      "./kn.js": "./node_modules/moment/locale/kn.js",
      "./ko": "./node_modules/moment/locale/ko.js",
      "./ko.js": "./node_modules/moment/locale/ko.js",
      "./ky": "./node_modules/moment/locale/ky.js",
      "./ky.js": "./node_modules/moment/locale/ky.js",
      "./lb": "./node_modules/moment/locale/lb.js",
      "./lb.js": "./node_modules/moment/locale/lb.js",
      "./lo": "./node_modules/moment/locale/lo.js",
      "./lo.js": "./node_modules/moment/locale/lo.js",
      "./lt": "./node_modules/moment/locale/lt.js",
      "./lt.js": "./node_modules/moment/locale/lt.js",
      "./lv": "./node_modules/moment/locale/lv.js",
      "./lv.js": "./node_modules/moment/locale/lv.js",
      "./me": "./node_modules/moment/locale/me.js",
      "./me.js": "./node_modules/moment/locale/me.js",
      "./mi": "./node_modules/moment/locale/mi.js",
      "./mi.js": "./node_modules/moment/locale/mi.js",
      "./mk": "./node_modules/moment/locale/mk.js",
      "./mk.js": "./node_modules/moment/locale/mk.js",
      "./ml": "./node_modules/moment/locale/ml.js",
      "./ml.js": "./node_modules/moment/locale/ml.js",
      "./mn": "./node_modules/moment/locale/mn.js",
      "./mn.js": "./node_modules/moment/locale/mn.js",
      "./mr": "./node_modules/moment/locale/mr.js",
      "./mr.js": "./node_modules/moment/locale/mr.js",
      "./ms": "./node_modules/moment/locale/ms.js",
      "./ms-my": "./node_modules/moment/locale/ms-my.js",
      "./ms-my.js": "./node_modules/moment/locale/ms-my.js",
      "./ms.js": "./node_modules/moment/locale/ms.js",
      "./mt": "./node_modules/moment/locale/mt.js",
      "./mt.js": "./node_modules/moment/locale/mt.js",
      "./my": "./node_modules/moment/locale/my.js",
      "./my.js": "./node_modules/moment/locale/my.js",
      "./nb": "./node_modules/moment/locale/nb.js",
      "./nb.js": "./node_modules/moment/locale/nb.js",
      "./ne": "./node_modules/moment/locale/ne.js",
      "./ne.js": "./node_modules/moment/locale/ne.js",
      "./nl": "./node_modules/moment/locale/nl.js",
      "./nl-be": "./node_modules/moment/locale/nl-be.js",
      "./nl-be.js": "./node_modules/moment/locale/nl-be.js",
      "./nl.js": "./node_modules/moment/locale/nl.js",
      "./nn": "./node_modules/moment/locale/nn.js",
      "./nn.js": "./node_modules/moment/locale/nn.js",
      "./pa-in": "./node_modules/moment/locale/pa-in.js",
      "./pa-in.js": "./node_modules/moment/locale/pa-in.js",
      "./pl": "./node_modules/moment/locale/pl.js",
      "./pl.js": "./node_modules/moment/locale/pl.js",
      "./pt": "./node_modules/moment/locale/pt.js",
      "./pt-br": "./node_modules/moment/locale/pt-br.js",
      "./pt-br.js": "./node_modules/moment/locale/pt-br.js",
      "./pt.js": "./node_modules/moment/locale/pt.js",
      "./ro": "./node_modules/moment/locale/ro.js",
      "./ro.js": "./node_modules/moment/locale/ro.js",
      "./ru": "./node_modules/moment/locale/ru.js",
      "./ru.js": "./node_modules/moment/locale/ru.js",
      "./sd": "./node_modules/moment/locale/sd.js",
      "./sd.js": "./node_modules/moment/locale/sd.js",
      "./se": "./node_modules/moment/locale/se.js",
      "./se.js": "./node_modules/moment/locale/se.js",
      "./si": "./node_modules/moment/locale/si.js",
      "./si.js": "./node_modules/moment/locale/si.js",
      "./sk": "./node_modules/moment/locale/sk.js",
      "./sk.js": "./node_modules/moment/locale/sk.js",
      "./sl": "./node_modules/moment/locale/sl.js",
      "./sl.js": "./node_modules/moment/locale/sl.js",
      "./sq": "./node_modules/moment/locale/sq.js",
      "./sq.js": "./node_modules/moment/locale/sq.js",
      "./sr": "./node_modules/moment/locale/sr.js",
      "./sr-cyrl": "./node_modules/moment/locale/sr-cyrl.js",
      "./sr-cyrl.js": "./node_modules/moment/locale/sr-cyrl.js",
      "./sr.js": "./node_modules/moment/locale/sr.js",
      "./ss": "./node_modules/moment/locale/ss.js",
      "./ss.js": "./node_modules/moment/locale/ss.js",
      "./sv": "./node_modules/moment/locale/sv.js",
      "./sv.js": "./node_modules/moment/locale/sv.js",
      "./sw": "./node_modules/moment/locale/sw.js",
      "./sw.js": "./node_modules/moment/locale/sw.js",
      "./ta": "./node_modules/moment/locale/ta.js",
      "./ta.js": "./node_modules/moment/locale/ta.js",
      "./te": "./node_modules/moment/locale/te.js",
      "./te.js": "./node_modules/moment/locale/te.js",
      "./tet": "./node_modules/moment/locale/tet.js",
      "./tet.js": "./node_modules/moment/locale/tet.js",
      "./tg": "./node_modules/moment/locale/tg.js",
      "./tg.js": "./node_modules/moment/locale/tg.js",
      "./th": "./node_modules/moment/locale/th.js",
      "./th.js": "./node_modules/moment/locale/th.js",
      "./tl-ph": "./node_modules/moment/locale/tl-ph.js",
      "./tl-ph.js": "./node_modules/moment/locale/tl-ph.js",
      "./tlh": "./node_modules/moment/locale/tlh.js",
      "./tlh.js": "./node_modules/moment/locale/tlh.js",
      "./tr": "./node_modules/moment/locale/tr.js",
      "./tr.js": "./node_modules/moment/locale/tr.js",
      "./tzl": "./node_modules/moment/locale/tzl.js",
      "./tzl.js": "./node_modules/moment/locale/tzl.js",
      "./tzm": "./node_modules/moment/locale/tzm.js",
      "./tzm-latn": "./node_modules/moment/locale/tzm-latn.js",
      "./tzm-latn.js": "./node_modules/moment/locale/tzm-latn.js",
      "./tzm.js": "./node_modules/moment/locale/tzm.js",
      "./ug-cn": "./node_modules/moment/locale/ug-cn.js",
      "./ug-cn.js": "./node_modules/moment/locale/ug-cn.js",
      "./uk": "./node_modules/moment/locale/uk.js",
      "./uk.js": "./node_modules/moment/locale/uk.js",
      "./ur": "./node_modules/moment/locale/ur.js",
      "./ur.js": "./node_modules/moment/locale/ur.js",
      "./uz": "./node_modules/moment/locale/uz.js",
      "./uz-latn": "./node_modules/moment/locale/uz-latn.js",
      "./uz-latn.js": "./node_modules/moment/locale/uz-latn.js",
      "./uz.js": "./node_modules/moment/locale/uz.js",
      "./vi": "./node_modules/moment/locale/vi.js",
      "./vi.js": "./node_modules/moment/locale/vi.js",
      "./x-pseudo": "./node_modules/moment/locale/x-pseudo.js",
      "./x-pseudo.js": "./node_modules/moment/locale/x-pseudo.js",
      "./yo": "./node_modules/moment/locale/yo.js",
      "./yo.js": "./node_modules/moment/locale/yo.js",
      "./zh-cn": "./node_modules/moment/locale/zh-cn.js",
      "./zh-cn.js": "./node_modules/moment/locale/zh-cn.js",
      "./zh-hk": "./node_modules/moment/locale/zh-hk.js",
      "./zh-hk.js": "./node_modules/moment/locale/zh-hk.js",
      "./zh-tw": "./node_modules/moment/locale/zh-tw.js",
      "./zh-tw.js": "./node_modules/moment/locale/zh-tw.js"
    };

    function webpackContext(req) {
      var id = webpackContextResolve(req);
      return __webpack_require__(id);
    }

    function webpackContextResolve(req) {
      var id = map[req];
      if (!(id + 1)) { // check for number or string
        var e = new Error("Cannot find module '" + req + "'");
        e.code = 'MODULE_NOT_FOUND';
        throw e;
      }
      return id;
    }

    webpackContext.keys = function webpackContextKeys() {
      return Object.keys(map);
    };
    webpackContext.resolve = webpackContextResolve;
    module.exports = webpackContext;
    webpackContext.id = "./node_modules/moment/locale sync recursive ^\\.\\/.*$";

    /***/
  }),

  /***/
  "./node_modules/vue-loader/lib/template-compiler/index.js?{\"id\":\"data-v-2715fe5c\",\"hasScoped\":true,\"optionsId\":\"0\",\"buble\":{\"transforms\":{}}}!./node_modules/vue-loader/lib/selector.js?type=template&index=0!./src/components/HomePage.vue":
  /*!*****************************************************************************************************************************************************************************************************************************************!*\
    !*** ./node_modules/vue-loader/lib/template-compiler?{"id":"data-v-2715fe5c","hasScoped":true,"optionsId":"0","buble":{"transforms":{}}}!./node_modules/vue-loader/lib/selector.js?type=template&index=0!./src/components/HomePage.vue ***!
    \*****************************************************************************************************************************************************************************************************************************************/
  /*! exports provided: render, staticRenderFns */
  /***/ (function (module, __webpack_exports__, __webpack_require__) {

    "use strict";
    __webpack_require__.r(__webpack_exports__);
    /* harmony export (binding) */
    __webpack_require__.d(__webpack_exports__, "render", function () {
      return render;
    });
    /* harmony export (binding) */
    __webpack_require__.d(__webpack_exports__, "staticRenderFns", function () {
      return staticRenderFns;
    });
    var render = function () {
      var _vm = this;
      var _h = _vm.$createElement;
      var _c = _vm._self._c || _h;
      return _c('div',
          [_c('EdgeHeader', {attrs: {"color": "teal darken-1"}}), _vm._v(" "),
            _c('div', {staticClass: "container free-bird"},
                _vm._l((_vm.assets), function (asset) {
                  return _c('row', [_c('column', {
                    staticClass: "mx-auto float-none white z-depth-1 py-2 px-2",
                    attrs: {"md": "10"}
                  }, [_c('card-body', [_c('router-link', {
                        attrs: {
                          "to": {
                            name: 'Asset',
                            params: {id: asset.entityRef.tag}
                          }
                        }
                      }, [_c('a', [_c('h2', {staticClass: "h2-responsive pb-4"},
                      [_c('strong', [_vm._v(_vm._s(asset.name))])])])]),
                        _vm._v(" "), _c('div', [_c('b', [_vm._v("URI: ")]),
                          _vm._v(_vm._s(asset.entityRef.uri) + "\n          ")]),
                        _vm._v(" "), _c('div', [_c('b', [_vm._v("Version URI: ")]),
                          _vm._v(
                              _vm._s(asset.entityRef.versionId) + "\n          ")]),
                        _vm._v(" "), _c('div', [_c('b', [_vm._v("Tag: ")]),
                          _vm._v(_vm._s(asset.entityRef.tag) + "\n          ")]),
                        _vm._v(" "), _c('div', [_c('b', [_vm._v("Version: ")]),
                          _vm._v(
                              _vm._s(asset.entityRef.version) + "\n          ")])],
                      1)], 1)], 1)
                }))], 1)
    }
    var staticRenderFns = []

    /***/
  }),

  /***/
  "./node_modules/vue-loader/lib/template-compiler/index.js?{\"id\":\"data-v-565ac3f2\",\"hasScoped\":false,\"optionsId\":\"0\",\"buble\":{\"transforms\":{}}}!./node_modules/vue-loader/lib/selector.js?type=template&index=0!./src/App.vue":
  /*!**************************************************************************************************************************************************************************************************************************!*\
    !*** ./node_modules/vue-loader/lib/template-compiler?{"id":"data-v-565ac3f2","hasScoped":false,"optionsId":"0","buble":{"transforms":{}}}!./node_modules/vue-loader/lib/selector.js?type=template&index=0!./src/App.vue ***!
    \**************************************************************************************************************************************************************************************************************************/
  /*! exports provided: render, staticRenderFns */
  /***/ (function (module, __webpack_exports__, __webpack_require__) {

    "use strict";
    __webpack_require__.r(__webpack_exports__);
    /* harmony export (binding) */
    __webpack_require__.d(__webpack_exports__, "render", function () {
      return render;
    });
    /* harmony export (binding) */
    __webpack_require__.d(__webpack_exports__, "staticRenderFns", function () {
      return staticRenderFns;
    });
    var render = function () {
      var _vm = this;
      var _h = _vm.$createElement;
      var _c = _vm._self._c || _h;
      return _c('div', {staticClass: "flyout", attrs: {"id": "app"}},
          [_c('navbar', {
            staticClass: "default-color",
            attrs: {"dark": "", "position": "top", "scrolling": ""}
          }, [_c('mdb-navbar-brand',
              {staticStyle: {"font-weight": "bolder"}, attrs: {"href": "#/"}},
              [_vm._v("\n      MEA 3D Semantic Repository\n    ")]),
            _vm._v(" "), _c('navbar-collapse',
                [_c('navbar-nav', {attrs: {"right": ""}}, [_c('navbar-item', {
                  attrs: {
                    "router": "",
                    "exact": "",
                    "href": "/",
                    "waves-fixed": ""
                  }
                }, [_vm._v("Home")])], 1)], 1)], 1), _vm._v(" "),
            _c('main', {style: ({marginTop: '60px'})}, [_c('router-view')], 1),
            _vm._v(" "), _c('Footer', {attrs: {"color": "default-color"}},
              [_c('p', {staticClass: "footer-copyright mb-0 py-3 text-center"},
                  [_vm._v("\n      © " + _vm._s(new Date().getFullYear())
                      + " Copyright: Mayo Clinic\n    ")])])], 1)
    }
    var staticRenderFns = []

    /***/
  }),

  /***/
  "./node_modules/vue-loader/lib/template-compiler/index.js?{\"id\":\"data-v-6be0f6b0\",\"hasScoped\":true,\"optionsId\":\"0\",\"buble\":{\"transforms\":{}}}!./node_modules/vue-loader/lib/selector.js?type=template&index=0!./src/components/Asset.vue":
  /*!**************************************************************************************************************************************************************************************************************************************!*\
    !*** ./node_modules/vue-loader/lib/template-compiler?{"id":"data-v-6be0f6b0","hasScoped":true,"optionsId":"0","buble":{"transforms":{}}}!./node_modules/vue-loader/lib/selector.js?type=template&index=0!./src/components/Asset.vue ***!
    \**************************************************************************************************************************************************************************************************************************************/
  /*! exports provided: render, staticRenderFns */
  /***/ (function (module, __webpack_exports__, __webpack_require__) {

    "use strict";
    __webpack_require__.r(__webpack_exports__);
    /* harmony export (binding) */
    __webpack_require__.d(__webpack_exports__, "render", function () {
      return render;
    });
    /* harmony export (binding) */
    __webpack_require__.d(__webpack_exports__, "staticRenderFns", function () {
      return staticRenderFns;
    });
    var render = function () {
      var _vm = this;
      var _h = _vm.$createElement;
      var _c = _vm._self._c || _h;
      return _c('div',
          [_c('EdgeHeader', {attrs: {"color": "teal darken-1"}}), _vm._v(" "),
            _c('div', {staticClass: "container free-bird"}, [_c('row',
                [_c('column', {
                  staticClass: "mx-auto float-none white z-depth-1 py-2 px-2",
                  attrs: {"md": "10"}
                }, [(_vm.asset) ? _c('card-body', [_c('a',
                    [_c('h2', {staticClass: "h2-responsive pb-4"},
                        [_c('strong', [_vm._v(_vm._s(_vm.asset.name))])])]),
                  _vm._v(" "), _c('div', [_c('b', [_vm._v("URI: ")]),
                    _vm._v(_vm._s(_vm.asset.resourceId.uri) + "\n          ")]),
                  _vm._v(" "), _c('div', [_c('b', [_vm._v("Version URI: ")]),
                    _vm._v(_vm._s(_vm.asset.resourceId.versionId)
                        + "\n          ")]), _vm._v(" "), _c('div',
                      [_c('b', [_vm._v("Tag: ")]), _vm._v(
                          _vm._s(_vm.asset.resourceId.tag) + "\n          ")]),
                  _vm._v(" "), _c('div', [_c('b', [_vm._v("Version: ")]),
                    _vm._v(_vm._s(_vm.asset.resourceId.version)
                        + "\n          ")]), _vm._v(" "), _c('div',
                      [_c('b', [_vm._v("Category: ")]), _vm._v(
                          _vm._s(_vm.asset.category["0"].label)
                          + "\n          ")])]) : _vm._e()], 1)], 1),
              _vm._v(" "), _c('row', [_c('column', {
                staticClass: "mx-auto float-none white z-depth-1 py-2 px-2",
                attrs: {"md": "10"}
              }, [(_vm.carrier) ? _c('card-body', [_c('h4',
                  [_c('b', [_vm._v("Language:")]), _vm._v(
                      " " + _vm._s(_vm.carrier.representation.language.uri))]),
                    _vm._v(" "), _c('pre', [_vm._v(
                    _vm._s(_vm.decode(_vm.carrier.encodedExpression)))])])
                  : _vm._e()], 1)], 1)], 1)], 1)
    }
    var staticRenderFns = []

    /***/
  }),

  /***/
  "./src/App.vue":
  /*!*********************!*\
    !*** ./src/App.vue ***!
    \*********************/
  /*! exports provided: default */
  /***/ (function (module, __webpack_exports__, __webpack_require__) {

    "use strict";
    __webpack_require__.r(__webpack_exports__);
    /* harmony import */
    var _babel_loader_node_modules_vue_loader_lib_selector_type_script_index_0_App_vue__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! !babel-loader!../node_modules/vue-loader/lib/selector?type=script&index=0!./App.vue */
        "./node_modules/babel-loader/lib/index.js!./node_modules/vue-loader/lib/selector.js?type=script&index=0!./src/App.vue");
    /* empty/unused harmony star reexport *//* harmony import */
    var _node_modules_vue_loader_lib_template_compiler_index_id_data_v_565ac3f2_hasScoped_false_optionsId_0_buble_transforms_node_modules_vue_loader_lib_selector_type_template_index_0_App_vue__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! !../node_modules/vue-loader/lib/template-compiler/index?{"id":"data-v-565ac3f2","hasScoped":false,"optionsId":"0","buble":{"transforms":{}}}!../node_modules/vue-loader/lib/selector?type=template&index=0!./App.vue */
        "./node_modules/vue-loader/lib/template-compiler/index.js?{\"id\":\"data-v-565ac3f2\",\"hasScoped\":false,\"optionsId\":\"0\",\"buble\":{\"transforms\":{}}}!./node_modules/vue-loader/lib/selector.js?type=template&index=0!./src/App.vue");
    /* harmony import */
    var _node_modules_vue_loader_lib_runtime_component_normalizer__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! ../node_modules/vue-loader/lib/runtime/component-normalizer */
        "./node_modules/vue-loader/lib/runtime/component-normalizer.js");

    function injectStyle(context) {
      __webpack_require__(/*! !../node_modules/extract-text-webpack-plugin/dist/loader.js?{"omit":1,"remove":true}!vue-style-loader!css-loader?{"sourceMap":true}!../node_modules/vue-loader/lib/style-compiler/index?{"optionsId":"0","vue":true,"scoped":false,"sourceMap":false}!../node_modules/vue-loader/lib/selector?type=styles&index=0!./App.vue */
          "./node_modules/extract-text-webpack-plugin/dist/loader.js?{\"omit\":1,\"remove\":true}!./node_modules/vue-style-loader/index.js!./node_modules/css-loader/index.js?{\"sourceMap\":true}!./node_modules/vue-loader/lib/style-compiler/index.js?{\"optionsId\":\"0\",\"vue\":true,\"scoped\":false,\"sourceMap\":false}!./node_modules/vue-loader/lib/selector.js?type=styles&index=0!./src/App.vue")
    }

    /* script */

    /* template */

    /* template functional */
    var __vue_template_functional__ = false
    /* styles */
    var __vue_styles__ = injectStyle
    /* scopeId */
    var __vue_scopeId__ = null
    /* moduleIdentifier (server only) */
    var __vue_module_identifier__ = null

    var Component = Object(
        _node_modules_vue_loader_lib_runtime_component_normalizer__WEBPACK_IMPORTED_MODULE_2__["default"])(
        _babel_loader_node_modules_vue_loader_lib_selector_type_script_index_0_App_vue__WEBPACK_IMPORTED_MODULE_0__["default"],
        _node_modules_vue_loader_lib_template_compiler_index_id_data_v_565ac3f2_hasScoped_false_optionsId_0_buble_transforms_node_modules_vue_loader_lib_selector_type_template_index_0_App_vue__WEBPACK_IMPORTED_MODULE_1__["render"],
        _node_modules_vue_loader_lib_template_compiler_index_id_data_v_565ac3f2_hasScoped_false_optionsId_0_buble_transforms_node_modules_vue_loader_lib_selector_type_template_index_0_App_vue__WEBPACK_IMPORTED_MODULE_1__["staticRenderFns"],
        __vue_template_functional__,
        __vue_styles__,
        __vue_scopeId__,
        __vue_module_identifier__
    )

    /* harmony default export */
    __webpack_exports__["default"] = (Component.exports);

    /***/
  }),

  /***/
  "./src/components/Asset.vue":
  /*!**********************************!*\
    !*** ./src/components/Asset.vue ***!
    \**********************************/
  /*! exports provided: default */
  /***/ (function (module, __webpack_exports__, __webpack_require__) {

    "use strict";
    __webpack_require__.r(__webpack_exports__);
    /* harmony import */
    var _babel_loader_node_modules_vue_loader_lib_selector_type_script_index_0_Asset_vue__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! !babel-loader!../../node_modules/vue-loader/lib/selector?type=script&index=0!./Asset.vue */
        "./node_modules/babel-loader/lib/index.js!./node_modules/vue-loader/lib/selector.js?type=script&index=0!./src/components/Asset.vue");
    /* empty/unused harmony star reexport *//* harmony import */
    var _node_modules_vue_loader_lib_template_compiler_index_id_data_v_6be0f6b0_hasScoped_true_optionsId_0_buble_transforms_node_modules_vue_loader_lib_selector_type_template_index_0_Asset_vue__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! !../../node_modules/vue-loader/lib/template-compiler/index?{"id":"data-v-6be0f6b0","hasScoped":true,"optionsId":"0","buble":{"transforms":{}}}!../../node_modules/vue-loader/lib/selector?type=template&index=0!./Asset.vue */
        "./node_modules/vue-loader/lib/template-compiler/index.js?{\"id\":\"data-v-6be0f6b0\",\"hasScoped\":true,\"optionsId\":\"0\",\"buble\":{\"transforms\":{}}}!./node_modules/vue-loader/lib/selector.js?type=template&index=0!./src/components/Asset.vue");
    /* harmony import */
    var _node_modules_vue_loader_lib_runtime_component_normalizer__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! ../../node_modules/vue-loader/lib/runtime/component-normalizer */
        "./node_modules/vue-loader/lib/runtime/component-normalizer.js");

    function injectStyle(context) {
      __webpack_require__(/*! !../../node_modules/extract-text-webpack-plugin/dist/loader.js?{"omit":1,"remove":true}!vue-style-loader!css-loader?{"sourceMap":true}!../../node_modules/vue-loader/lib/style-compiler/index?{"optionsId":"0","vue":true,"id":"data-v-6be0f6b0","scoped":true,"sourceMap":false}!../../node_modules/vue-loader/lib/selector?type=styles&index=0!./Asset.vue */
          "./node_modules/extract-text-webpack-plugin/dist/loader.js?{\"omit\":1,\"remove\":true}!./node_modules/vue-style-loader/index.js!./node_modules/css-loader/index.js?{\"sourceMap\":true}!./node_modules/vue-loader/lib/style-compiler/index.js?{\"optionsId\":\"0\",\"vue\":true,\"id\":\"data-v-6be0f6b0\",\"scoped\":true,\"sourceMap\":false}!./node_modules/vue-loader/lib/selector.js?type=styles&index=0!./src/components/Asset.vue")
    }

    /* script */

    /* template */

    /* template functional */
    var __vue_template_functional__ = false
    /* styles */
    var __vue_styles__ = injectStyle
    /* scopeId */
    var __vue_scopeId__ = "data-v-6be0f6b0"
    /* moduleIdentifier (server only) */
    var __vue_module_identifier__ = null

    var Component = Object(
        _node_modules_vue_loader_lib_runtime_component_normalizer__WEBPACK_IMPORTED_MODULE_2__["default"])(
        _babel_loader_node_modules_vue_loader_lib_selector_type_script_index_0_Asset_vue__WEBPACK_IMPORTED_MODULE_0__["default"],
        _node_modules_vue_loader_lib_template_compiler_index_id_data_v_6be0f6b0_hasScoped_true_optionsId_0_buble_transforms_node_modules_vue_loader_lib_selector_type_template_index_0_Asset_vue__WEBPACK_IMPORTED_MODULE_1__["render"],
        _node_modules_vue_loader_lib_template_compiler_index_id_data_v_6be0f6b0_hasScoped_true_optionsId_0_buble_transforms_node_modules_vue_loader_lib_selector_type_template_index_0_Asset_vue__WEBPACK_IMPORTED_MODULE_1__["staticRenderFns"],
        __vue_template_functional__,
        __vue_styles__,
        __vue_scopeId__,
        __vue_module_identifier__
    )

    /* harmony default export */
    __webpack_exports__["default"] = (Component.exports);

    /***/
  }),

  /***/
  "./src/components/HomePage.vue":
  /*!*************************************!*\
    !*** ./src/components/HomePage.vue ***!
    \*************************************/
  /*! exports provided: default */
  /***/ (function (module, __webpack_exports__, __webpack_require__) {

    "use strict";
    __webpack_require__.r(__webpack_exports__);
    /* harmony import */
    var _babel_loader_node_modules_vue_loader_lib_selector_type_script_index_0_HomePage_vue__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! !babel-loader!../../node_modules/vue-loader/lib/selector?type=script&index=0!./HomePage.vue */
        "./node_modules/babel-loader/lib/index.js!./node_modules/vue-loader/lib/selector.js?type=script&index=0!./src/components/HomePage.vue");
    /* empty/unused harmony star reexport *//* harmony import */
    var _node_modules_vue_loader_lib_template_compiler_index_id_data_v_2715fe5c_hasScoped_true_optionsId_0_buble_transforms_node_modules_vue_loader_lib_selector_type_template_index_0_HomePage_vue__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! !../../node_modules/vue-loader/lib/template-compiler/index?{"id":"data-v-2715fe5c","hasScoped":true,"optionsId":"0","buble":{"transforms":{}}}!../../node_modules/vue-loader/lib/selector?type=template&index=0!./HomePage.vue */
        "./node_modules/vue-loader/lib/template-compiler/index.js?{\"id\":\"data-v-2715fe5c\",\"hasScoped\":true,\"optionsId\":\"0\",\"buble\":{\"transforms\":{}}}!./node_modules/vue-loader/lib/selector.js?type=template&index=0!./src/components/HomePage.vue");
    /* harmony import */
    var _node_modules_vue_loader_lib_runtime_component_normalizer__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! ../../node_modules/vue-loader/lib/runtime/component-normalizer */
        "./node_modules/vue-loader/lib/runtime/component-normalizer.js");

    function injectStyle(context) {
      __webpack_require__(/*! !../../node_modules/extract-text-webpack-plugin/dist/loader.js?{"omit":1,"remove":true}!vue-style-loader!css-loader?{"sourceMap":true}!../../node_modules/vue-loader/lib/style-compiler/index?{"optionsId":"0","vue":true,"id":"data-v-2715fe5c","scoped":true,"sourceMap":false}!../../node_modules/vue-loader/lib/selector?type=styles&index=0!./HomePage.vue */
          "./node_modules/extract-text-webpack-plugin/dist/loader.js?{\"omit\":1,\"remove\":true}!./node_modules/vue-style-loader/index.js!./node_modules/css-loader/index.js?{\"sourceMap\":true}!./node_modules/vue-loader/lib/style-compiler/index.js?{\"optionsId\":\"0\",\"vue\":true,\"id\":\"data-v-2715fe5c\",\"scoped\":true,\"sourceMap\":false}!./node_modules/vue-loader/lib/selector.js?type=styles&index=0!./src/components/HomePage.vue")
    }

    /* script */

    /* template */

    /* template functional */
    var __vue_template_functional__ = false
    /* styles */
    var __vue_styles__ = injectStyle
    /* scopeId */
    var __vue_scopeId__ = "data-v-2715fe5c"
    /* moduleIdentifier (server only) */
    var __vue_module_identifier__ = null

    var Component = Object(
        _node_modules_vue_loader_lib_runtime_component_normalizer__WEBPACK_IMPORTED_MODULE_2__["default"])(
        _babel_loader_node_modules_vue_loader_lib_selector_type_script_index_0_HomePage_vue__WEBPACK_IMPORTED_MODULE_0__["default"],
        _node_modules_vue_loader_lib_template_compiler_index_id_data_v_2715fe5c_hasScoped_true_optionsId_0_buble_transforms_node_modules_vue_loader_lib_selector_type_template_index_0_HomePage_vue__WEBPACK_IMPORTED_MODULE_1__["render"],
        _node_modules_vue_loader_lib_template_compiler_index_id_data_v_2715fe5c_hasScoped_true_optionsId_0_buble_transforms_node_modules_vue_loader_lib_selector_type_template_index_0_HomePage_vue__WEBPACK_IMPORTED_MODULE_1__["staticRenderFns"],
        __vue_template_functional__,
        __vue_styles__,
        __vue_scopeId__,
        __vue_module_identifier__
    )

    /* harmony default export */
    __webpack_exports__["default"] = (Component.exports);

    /***/
  }),

  /***/
  "./src/main.js":
  /*!*********************!*\
    !*** ./src/main.js ***!
    \*********************/
  /*! no exports provided */
  /***/ (function (module, __webpack_exports__, __webpack_require__) {

    "use strict";
    __webpack_require__.r(__webpack_exports__);
    /* harmony import */
    var bootstrap_css_only_css_bootstrap_min_css__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! bootstrap-css-only/css/bootstrap.min.css */
        "./node_modules/bootstrap-css-only/css/bootstrap.min.css");
    /* harmony import */
    var bootstrap_css_only_css_bootstrap_min_css__WEBPACK_IMPORTED_MODULE_0___default = /*#__PURE__*/__webpack_require__.n(
        bootstrap_css_only_css_bootstrap_min_css__WEBPACK_IMPORTED_MODULE_0__);
    /* harmony import */
    var mdbvue_build_css_mdb_css__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! mdbvue/build/css/mdb.css */
        "./node_modules/mdbvue/build/css/mdb.css");
    /* harmony import */
    var mdbvue_build_css_mdb_css__WEBPACK_IMPORTED_MODULE_1___default = /*#__PURE__*/__webpack_require__.n(
        mdbvue_build_css_mdb_css__WEBPACK_IMPORTED_MODULE_1__);
    /* harmony import */
    var vue__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! vue */
        "./node_modules/vue/dist/vue.esm.js");
    /* harmony import */
    var _App__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! ./App */
        "./src/App.vue");
    /* harmony import */
    var _router__WEBPACK_IMPORTED_MODULE_4__ = __webpack_require__(/*! ./router */
        "./src/router/index.js");
// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.

    vue__WEBPACK_IMPORTED_MODULE_2__["default"].config.productionTip = false;

    /* eslint-disable no-new */
    new vue__WEBPACK_IMPORTED_MODULE_2__["default"]({
      el: '#app',
      router: _router__WEBPACK_IMPORTED_MODULE_4__["default"],
      components: {App: _App__WEBPACK_IMPORTED_MODULE_3__["default"]},
      template: '<App/>'
    });

    /***/
  }),

  /***/
  "./src/router/index.js":
  /*!*****************************!*\
    !*** ./src/router/index.js ***!
    \*****************************/
  /*! exports provided: default */
  /***/ (function (module, __webpack_exports__, __webpack_require__) {

    "use strict";
    __webpack_require__.r(__webpack_exports__);
    /* harmony import */
    var vue__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! vue */
        "./node_modules/vue/dist/vue.esm.js");
    /* harmony import */
    var vue_router__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! vue-router */
        "./node_modules/vue-router/dist/vue-router.esm.js");
    /* harmony import */
    var _components_HomePage__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! @/components/HomePage */
        "./src/components/HomePage.vue");
    /* harmony import */
    var _components_Asset__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! @/components/Asset */
        "./src/components/Asset.vue");
    /* harmony import */
    var v_hotkey__WEBPACK_IMPORTED_MODULE_4__ = __webpack_require__(/*! v-hotkey */
        "./node_modules/v-hotkey/index.js");
    /* harmony import */
    var v_hotkey__WEBPACK_IMPORTED_MODULE_4___default = /*#__PURE__*/__webpack_require__.n(
        v_hotkey__WEBPACK_IMPORTED_MODULE_4__);

    vue__WEBPACK_IMPORTED_MODULE_0__["default"].use(
        vue_router__WEBPACK_IMPORTED_MODULE_1__["default"]);
    vue__WEBPACK_IMPORTED_MODULE_0__["default"].use(
        v_hotkey__WEBPACK_IMPORTED_MODULE_4___default.a);

    /* harmony default export */
    __webpack_exports__["default"] = (new vue_router__WEBPACK_IMPORTED_MODULE_1__["default"](
        {
          routes: [{
            path: '/',
            name: 'HomePage',
            component: _components_HomePage__WEBPACK_IMPORTED_MODULE_2__["default"]
          }, {
            path: '/assets/:id',
            name: 'Asset',
            component: _components_Asset__WEBPACK_IMPORTED_MODULE_3__["default"]
          }]
        }));

    /***/
  })

  /******/
});
//# sourceMappingURL=app.c7695e5416b93328747d.js.map