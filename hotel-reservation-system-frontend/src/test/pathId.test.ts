import { describe, expect, it } from 'vitest'
import { toPathId } from '../services/pathId'

describe('toPathId', () => {
  it('acepta enteros positivos como número o texto', () => {
    expect(toPathId(3)).toBe('3')
    expect(toPathId('42')).toBe('42')
  })

  it('rechaza valores que podrían alterar la ruta de la API', () => {
    for (const bad of ['../user/get-all', '3/../../x', '1?admin=true', '-1', '0', '1.5', '', null, undefined, NaN]) {
      expect(() => toPathId(bad)).toThrow('Identificador inválido')
    }
  })
})
