import DefaultTheme from 'vitepress/theme'
import CraftingRecipe from './components/CraftingRecipe.vue'

export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    app.component('CraftingRecipe', CraftingRecipe)
  }
}
