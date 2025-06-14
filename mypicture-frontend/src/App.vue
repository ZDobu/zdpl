<template>
  <div id="app">

    <a-config-provider :locale="locale">
      <BasicLayout />
    </a-config-provider>
  </div>

</template>

<script setup lang="ts">
import BasicLayout from './layouts/BasicLayout.vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';

// 设置 dayjs 中文语言
dayjs.locale('zh-cn');

// 直接定义响应式变量
const locale = zhCN;

</script>

<style scoped></style>
