import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('App', () => {
  it('deveExibirOTituloDaAplicacao', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: 'RPE Card Processing' })).toBeInTheDocument()
  })
})
