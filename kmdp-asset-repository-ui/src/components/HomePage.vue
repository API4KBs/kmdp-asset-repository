<template>
    <div>
      <EdgeHeader color="teal darken-1"/>

      <div class="container free-bird">
        <div style="text-align:center;">
          <btn color="primary" v-for="type in Object.keys(assets)" @click.native="setActive(type)" :active="active[type]">{{ type }}</btn>
        </div>

        <row v-for="asset in assetsList" :key="asset.entityRef.tag">
          <column md="10" class="mx-auto float-none white z-depth-1 py-2 px-2">
            <card-body>
              <router-link :to="{ name: 'Asset', params: { id: getTag(asset.entityRef) }}">
              <a>
                <h2 class="h2-responsive pb-4"><strong>{{ asset.name }}</strong></h2>
              </a>
              </router-link>
              <div>
                <b>URI: </b>{{ asset.entityRef.uri }}
              </div>
              <div>
                <b>Version URI: </b>{{ asset.entityRef.versionId }}
              </div>
              <div>
                <b>Type: </b>{{ asset.type }}
              </div>
            </card-body>
          </column>
        </row>
      </div>
    </div>
</template>

<script>
import { Container, Column, Row, Fa, Navbar, NavbarItem, NavbarNav, NavbarCollapse, EdgeHeader, CardBody, Btn, BtnGroup } from 'mdbvue';
import axios from 'axios';

export default {
  name: 'HomePage',
  components: {
    Container,
    Column,
    Row,
    Fa,
    Navbar,
    NavbarItem,
    NavbarNav,
    NavbarCollapse,
    EdgeHeader,
    CardBody,
    Btn,
    BtnGroup
  },
  created: function () {
    this.getAssets()
  },
  data() {
    return {
      active: { },
      assets: {
        "Unknown": []
      },
      assetsList: []
    }
  },
  methods: {
    setActive: function(type) {
      this.$set(this.active, type, !this.active[type])
      this.filterAssets()
    },
    filterAssets: function () {
      var assets = []
      for (var key in this.assets) {
        if (this.active[key]) {
          assets = assets.concat(this.assets[key])
        }
      }
      this.assetsList = assets
    },
    getTag: function(uri) {
      return uri.uri.split("/").pop();
    },
    getAssets: function () {
      axios.get('cat/assets')
          .then((assets) => {
            var assets = assets.data

            for (var i = 0; i < assets.length; i++) {
              var type = assets[i].type
              if (! type) {
                type = "Unknown"
              } else {
                var n = type.lastIndexOf('#');
                type = type.substring(n + 1);
              }
              if (! this.assets[type]) {
                this.assets[type] = []
              }

              this.assets[type].push(assets[i])
            }

            this.filterAssets();
          })
      .catch(e => {
          throw e;
      })
    }
  }
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h1, h2 {
  font-weight: normal;
}

.home-feature-box {
  padding: 40px 0;
}

.home-feature-box i {
  font-size: 6rem;
}

.home-feature-box span {
  display: block;
  color: black;
  font-size: 20px;
  font-weight: bold;
  padding-top: 20px;
}

ul {
  list-style-type: none;
  padding: 0;
}

li {
  display: inline-block;
}

a {
  color: #42b983;
}
.sentence {
  font-size: 24pt;
}
.charts {
  padding: 10px;
}
</style>
