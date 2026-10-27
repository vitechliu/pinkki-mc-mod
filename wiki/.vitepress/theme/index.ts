import GuabaBrewing from './components/GuabaBrewing.vue'
import DefaultTheme from 'vitepress/theme'
import CraftingRecipe from './components/CraftingRecipe.vue'
import GuabaCharging from './components/GuabaCharging.vue'
import GuabaConversion from './components/GuabaConversion.vue'
import SmeltingRecipe from './components/SmeltingRecipe.vue'

export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    app.component('GuabaBrewing', GuabaBrewing)
    app.component('CraftingRecipe', CraftingRecipe)
    app.component('GuabaConversion', GuabaConversion)
    app.component('GuabaCharging', GuabaCharging)
    app.component('SmeltingRecipe', SmeltingRecipe)
  }
}
