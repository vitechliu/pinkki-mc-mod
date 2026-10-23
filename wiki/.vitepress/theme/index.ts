import DefaultTheme from 'vitepress/theme'
import CraftingRecipe from './components/CraftingRecipe.vue'
import GuabaConversion from './components/GuabaConversion.vue'

export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    app.component('CraftingRecipe', CraftingRecipe)
    app.component('GuabaConversion', GuabaConversion)
  }
}
