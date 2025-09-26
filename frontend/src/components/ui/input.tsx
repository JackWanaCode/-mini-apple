import * as React from 'react'


export const Input = React.forwardRef<HTMLInputElement, React.InputHTMLAttributes<HTMLInputElement>>(function Input(
    { className = '', ...props }, ref
) {
    return (
    <input
    ref={ref}
    className={`border border-slate-300 rounded-xl px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-slate-300 ${className}`}
    {...props}
    />
    )
})