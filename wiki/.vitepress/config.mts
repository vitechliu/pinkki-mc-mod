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
      { text: '序章', items: [
        { text: '模组简介', link: '/guide' },
        { text: '瓜巴手册', link: '/handbook' },
        { text: '项目版本与模组依赖', link: '/guide#versions' }
      ] },
      { text: '材料', items: [
        { text: '瓜巴', link: '/items#guaba' },
        { text: '雷霆瓜巴', link: '/items#thunder-guaba' },
        { text: '粉红矿', link: '/items#pink-ore' },
        { text: '云梦与瓜巴云梦', link: '/items#cloud-dream' },
        { text: '冥梦与瓜巴冥梦', link: '/items#nether-dream' },
        { text: '装备材料联动', link: '/compatibility#guaba' },
        { text: '张哥之爱与旮旯之魂', link: '/galgame#zhang-fav' }
      ] },
      { text: '工作方块', items: [
        { text: '瓜巴工作台', link: '/guaba-workbench' },
        { text: '瓜巴充电桩', link: '/items#guaba-charging-station' },
        { text: '旮旯游戏机', link: '/galgame#galgame-arcade' }
      ] },
      { text: '基础武器', items: [
        { text: '瓜巴剑', link: '/items#guaba-sword' },
        { text: '瓜巴弓', link: '/items#guaba-bow' },
        { text: '饰品', link: '/items#guaba-hood' },
        { text: '粉饰', link: '/galgame#favorite-person' }
      ] },
      { text: '药水效果', items: [
        { text: '瓜巴药水', link: '/guaba-potions' },
        { text: '雷霆瓜巴药水', link: '/guaba-potions#thunder-guaba' },
        { text: '旮旯', link: '/galgame#galgame' }
      ] },
      { text: '生物', items: [
        { text: '瓜巴龙与变种', link: '/bosses#guaba-dragon' },
        { text: '粉红妹', link: '/bosses#pinkki' },
        { text: '瓜巴影融', link: '/bosses#guaba-yingrong' },
        { text: '饼蛙', link: '/bosses#bingwa' },
        { text: '闪电苦力怕', link: '/items#charged-creeper-spawn-egg' },
        { text: '阵营与野外掉落', link: '/bosses#factions' },
        { text: '张小弟与张哥', link: '/galgame#zhang' }
      ] },
      { text: '合成与加工', link: '/recipes' },
      { text: '更新记录', link: '/changelog' }
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
