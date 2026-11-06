import { describe, expect, it } from 'vitest'

import {
  formatNumber,
  ingredientLine,
  parseAmount,
  roundReadable,
  scaleAmount,
  scaleDishIngredients,
  scaleIngredients,
} from '@/utils/scale'
import type { Ingredient } from '@/types/api'

const ingredient = (name: string, amount: string, unit: string): Ingredient => ({
  name,
  amount,
  unit,
})

describe('parseAmount', () => {
  it('解析普通数字与小数', () => {
    expect(parseAmount('300')).toBe(300)
    expect(parseAmount('1.5')).toBe(1.5)
    expect(parseAmount(20)).toBe(20)
  })

  it('解析分数', () => {
    expect(parseAmount('1/2')).toBe(0.5)
    expect(parseAmount('1 1/2')).toBe(1.5)
  })

  it('解析区间取中间值', () => {
    expect(parseAmount('2-3')).toBe(2.5)
    expect(parseAmount('10~20')).toBe(15)
  })

  it('解析带前后缀与中文数字', () => {
    expect(parseAmount('约200')).toBe(200)
    expect(parseAmount('200左右')).toBe(200)
    expect(parseAmount('半')).toBe(0.5)
    expect(parseAmount('两')).toBe(2)
  })

  it('无法解析时返回 null', () => {
    expect(parseAmount('适量')).toBeNull()
    expect(parseAmount('少许')).toBeNull()
    expect(parseAmount('')).toBeNull()
    expect(parseAmount(null)).toBeNull()
    expect(parseAmount(undefined)).toBeNull()
  })
})

describe('roundReadable', () => {
  it('质量单位取易读倍数', () => {
    expect(roundReadable(450, 'g')).toBe(450)
    expect(roundReadable(333, 'g')).toBe(330)
    expect(roundReadable(24, 'g')).toBe(25)
    expect(roundReadable(12.4, 'ml')).toBe(12)
  })

  it('计件单位向上取整', () => {
    expect(roundReadable(3, '个')).toBe(3)
    expect(roundReadable(2.2, '只')).toBe(3)
    expect(roundReadable(0.4, '个')).toBe(1)
  })

  it('未知单位保留一位小数', () => {
    expect(roundReadable(1.24, '斤')).toBe(1.2)
    expect(roundReadable(0.04, '斤')).toBe(0.1)
  })
})

describe('scaleAmount', () => {
  it('无法解析的用量原样保留', () => {
    expect(scaleAmount('适量', 2, '')).toBe('适量')
    expect(scaleAmount('少许', 1.5, '')).toBe('少许')
  })

  it('按比例缩放并取易读值', () => {
    expect(scaleAmount('300', 1.5, 'g')).toBe('450')
    expect(scaleAmount('2', 1.5, '个')).toBe('3')
    expect(scaleAmount('100', 0.5, 'g')).toBe('50')
  })
})

describe('scaleIngredients', () => {
  const ingredients: Ingredient[] = [
    ingredient('五花肉', '300', 'g'),
    ingredient('鸡蛋', '2', '个'),
    ingredient('盐', '适量', ''),
  ]

  it('按人数 / 基准份量等比缩放', () => {
    const scaled = scaleIngredients(ingredients, 2, 3)
    expect(scaled[0]?.amount).toBe('450')
    expect(scaled[1]?.amount).toBe('3')
    expect(scaled[2]?.amount).toBe('适量')
  })

  it('同份量返回等值副本，不修改入参', () => {
    const scaled = scaleIngredients(ingredients, 2, 2)
    expect(scaled).toEqual(ingredients)
    expect(scaled).not.toBe(ingredients)
    expect(scaled[0]).not.toBe(ingredients[0])
  })

  it('基准份量非法时按 1 份处理', () => {
    const scaled = scaleIngredients([ingredient('米', '100', 'g')], 0, 2)
    expect(scaled[0]?.amount).toBe('200')
  })

  it('目标人数非法时回落到基准份量', () => {
    const scaled = scaleIngredients([ingredient('米', '100', 'g')], 2, 0)
    expect(scaled[0]?.amount).toBe('100')
  })

  it('空配料返回空数组', () => {
    expect(scaleIngredients([], 2, 3)).toEqual([])
  })
})

describe('scaleDishIngredients', () => {
  it('按 baseServings 与人数缩放菜品配料', () => {
    const scaled = scaleDishIngredients(
      {
        baseServings: 2,
        ingredients: [ingredient('面粉', '200', 'g')],
      },
      4,
    )
    expect(scaled[0]?.amount).toBe('400')
  })
})

describe('辅助格式化', () => {
  it('formatNumber 去掉多余小数', () => {
    expect(formatNumber(3)).toBe('3')
    expect(formatNumber(2.5)).toBe('2.5')
  })

  it('ingredientLine 拼接配料行', () => {
    expect(ingredientLine(ingredient('五花肉', '300', 'g'))).toBe('五花肉 300 g')
    expect(ingredientLine(ingredient('盐', '', ''))).toBe('盐')
  })
})
