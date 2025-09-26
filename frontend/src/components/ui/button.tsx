import * as React from 'react'


type ButtonProps = React.ButtonHTMLAttributes<HTMLButtonElement> & {
variant?: 'default' | 'outline'
}


export function Button({ variant = 'default', className = '', ...props }: ButtonProps) {
const base = 'px-3 py-2 rounded-xl text-sm shadow-sm active:translate-y-px disabled:opacity-50'
const styles = variant === 'outline'
? 'border border-slate-300 bg-white hover:bg-slate-50'
: 'bg-slate-900 text-white hover:bg-slate-800'
return <button className={`${base} ${styles} ${className}`} {...props} />
}
