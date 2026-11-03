<script setup lang="ts">
import { RouterView } from 'vue-router'
import GlobalErrorBoundary from './components/ui/GlobalErrorBoundary.vue'
import TopProgressBar from './components/ui/TopProgressBar.vue'
import OfflineAlertBanner from './components/ui/OfflineAlertBanner.vue'
import AppToastContainer from './components/ui/AppToastContainer.vue'
</script>

<template>
  <GlobalErrorBoundary>
    <TopProgressBar />
    <OfflineAlertBanner />
    <RouterView />
    <AppToastContainer />
  </GlobalErrorBoundary>
</template>
