type ItemIngredient = { item: string }
type TagIngredient = { tag: string }
export type RecipeIngredient = ItemIngredient | TagIngredient | {
  type: 'forge:difference'
  base: ItemIngredient | TagIngredient
  subtracted: ItemIngredient | TagIngredient
}

// A tag visual is a representative item. Matching and exclusions come from the recipe JSON.
export function ingredientKey(ingredient: RecipeIngredient): string {
  const base = 'base' in ingredient ? ingredient.base : ingredient
  return 'item' in base ? base.item : '#' + base.tag
}
