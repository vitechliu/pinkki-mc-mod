import { defineConfig } from 'vitepress'

export default defineConfig({
  lang: 'zh-CN',
  title: 'PinkkiCore Wiki',
  description: 'PinkkiCore 玩家指南：道具、合成、结构、多方块与 Boss。',
  base: '/pinkki-mc-mod/',
  cleanUrls: true,
  themeConfig: {
    nav: [
      { text: '首页', link: '/' },
      { text: '开始阅读', link: '/guide' }
    ],
    sidebar: [
      { text: '玩家指南', items: [
        { text: '项目与版本', link: '/guide' },
        { text: '道具与方块', link: '/items' },
        { text: '瓜巴工作台', link: '/guaba-workbench' },
        { text: '合成与加工', link: '/recipes' },
        { text: '模组兼容', link: '/compatibility' },
        { text: '世界结构', link: '/structures' },
        { text: '多方块', link: '/multiblocks' },
        { text: '生物与 Boss', link: '/bosses' },
        { text: '更新记录', link: '/changelog' }
      ] }
    ],
    search: { provider: 'local', options: { locales: { root: { translations: {
      button: { buttonText: '搜索文档', buttonAriaLabel: '搜索文档' },
      modal: { noResultsText: '没有找到相关内容', resetButtonTitle: '清除搜索',
        footer: { selectText: '选择', navigateText: '切换', closeText: '关闭' } }
    } } } } },
    socialLinks: [{ icon: 'github', link: 'https://github.com/vitechliu/pinkki-mc-mod' }],
    outline: { label: '本页目录' },
    docFooter: { prev: '上一页', next: '下一页' },
    sidebarMenuLabel: '目录',
    returnToTopLabel: '返回顶部',
    darkModeSwitchLabel: '外观',
    footer: { message: 'PinkkiCore · Minecraft Java 版模组', copyright: 'MIT License' }
  }
})
