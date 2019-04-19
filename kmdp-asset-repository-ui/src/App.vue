<template>
  <div id="app" class="flyout">
    <navbar dark position="top" class="default-color" scrolling>
      <mdb-navbar-brand href="#/" style="font-weight: bolder;">
        MEA 3D Semantic Repository
      </mdb-navbar-brand>
      <navbar-collapse>
        <navbar-nav right>
          <navbar-item router exact href="/" waves-fixed>Home</navbar-item>
        </navbar-nav>
      </navbar-collapse>
    </navbar>
    <main :style="{marginTop: '60px'}">
      <router-view></router-view>
    </main>
    <Footer color="default-color">
      <p class="footer-copyright mb-0 py-3 text-center">
        &copy; {{new Date().getFullYear()}} Copyright: Mayo Clinic
      </p>
    </Footer>
  </div>
</template>

<script>
  import { Navbar, NavbarItem, NavbarNav, NavbarCollapse, mdbNavbarBrand, Footer } from 'mdbvue';

  export default {
    name: 'app',
    components: {
      Navbar,
      NavbarItem,
      NavbarNav,
      NavbarCollapse,
      mdbNavbarBrand,
      Footer
    }
  };

</script>

<style>
  .flyout {
    display:flex;
    flex-direction: column;
    min-height:100vh;
    justify-content: space-between;
  }

  .active{
    background-color: rgba(255, 255, 255, 0.1);
  }

</style>
