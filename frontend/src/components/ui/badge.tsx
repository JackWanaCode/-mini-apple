import * as React from 'react'


type BadgeProps = React.HTMLAttributes<HTMLSpanElement> & { variant?: 'default' | 'secondary' }


export function Badge({ variant = 'default', className = '', ...props }: BadgeProps) {
const styles = variant === 'secondary'
? 'bg-slate-100 text-slate-700 border border-slate-200'
: 'bg-slate-900 text-white'
return <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs ${styles} ${className}`} {...props} />
}