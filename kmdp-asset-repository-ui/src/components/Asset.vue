<template>
  <div>
    <EdgeHeader color="teal darken-1"/>
    <div class="container free-bird">
      <row>
        <column md="10" class="mx-auto float-none white z-depth-1 py-2 px-2">
          <card-body v-if="asset">
            <a>
              <h2 class="h2-responsive pb-4"><strong>{{ asset.name }}</strong></h2>
            </a>
            <div>
              <b>URI: </b>{{ asset.resourceId.uri }}
            </div>
            <div>
              <b>Version URI: </b>{{ asset.resourceId.versionId }}
            </div>
            <div>
              <b>Category: </b>{{ asset.category["0"].label }}
            </div>
          </card-body>
        </column>
      </row>

      <row>
        <column md="10" class="mx-auto float-none white z-depth-1 py-2 px-2">
          <card-body v-if="asset">
            <h2><b>Surrogate</b></h2>
            <pre v-highlightjs><code class="json">{{ asset }}</code></pre>
          </card-body>
        </column>

        <column md="10" class="mx-auto float-none white z-depth-1 py-2 px-2">
          <card-body v-if="carrier">
            <h2><b>Artifact</b></h2>
            <h4><b>Language:</b> {{ carrier.representation.language.label }}</h4>
            <pre v-highlightjs><code v-bind:class="classObject">{{ decode(carrier.encodedExpression) }}</code></pre>
          </card-body>
        </column>
      </row>
    </div>
  </div>
</template>

<script>
import { Container, Column, Row, Fa, Navbar, NavbarItem, NavbarNav, NavbarCollapse, Btn, EdgeHeader, CardBody } from 'mdbvue';
import axios from 'axios';

export default {
  name: 'Asset',
  components: {
    Container,
    Column,
    Row,
    Fa,
    Navbar,
    NavbarItem,
    NavbarNav,
    NavbarCollapse,
    Btn,
    EdgeHeader,
    CardBody
  },
  created: function () {
    this.getAsset()
  },
  data() {
    return {
      asset: null,
      carrier: null
    }
  },
  computed: {
    classObject: function () {
      //TODO:
      return 'xml';
    }
  },
  methods: {
    decode : function(s) {
      return atob(s);
    },
    getAsset: function () {
      axios.get('cat/assets/' + this.$route.params.id )
          .then((asset) => {
            this.asset = asset.data;

            var parts = this.asset.resourceId.versionId.split("/");

            var version = parts[parts.length - 1]
            var tag = parts[parts.length - 3]

            axios.get('cat/assets/' + tag + '/versions/' + version + '/carrier')
              .then((carrier) => {
                this.carrier = carrier.data
              })
              .catch(e => {
                throw e;
              })
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
