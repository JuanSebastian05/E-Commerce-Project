import type { ButtonHTMLAttributes } from 'react'

type Variant = 'primary' | 'secondary' | 'danger' | 'link'

const VARIANTS: Record<Variant, string> = {
  primary: 'rounded-md bg-blue-600 px-3 py-1.5 font-medium text-white hover:bg-blue-700',
  secondary: 'rounded-md border border-gray-300 bg-white px-3 py-1.5 font-medium text-gray-700 hover:bg-gray-50',
  danger: 'rounded-md bg-red-600 px-3 py-1.5 font-medium text-white hover:bg-red-700',
  link: 'font-medium text-blue-600 hover:underline',
}

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
}

export function Button({ variant = 'primary', className = '', type = 'button', ...props }: ButtonProps) {
  return (
    <button
      type={type}
      className={`text-sm disabled:cursor-not-allowed disabled:opacity-50 ${VARIANTS[variant]} ${className}`}
      {...props}
    />
  )
}
