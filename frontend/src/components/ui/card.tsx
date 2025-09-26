import * as React from 'react'


export function Card({ className = '', ...props }: React.HTMLAttributes<HTMLDivElement>) {
return <div className={`bg-white rounded-2xl border border-slate-200 ${className}`} {...props} />
}
export function CardHeader({ className = '', ...props }: React.HTMLAttributes<HTMLDivElement>) {
return <div className={`p-4 border-b border-slate-100 ${className}`} {...props} />
}
export function CardTitle({ className = '', ...props }: React.HTMLAttributes<HTMLHeadingElement>) {
return <h3 className={`font-semibold text-slate-900 ${className}`} {...props} />
}
export function CardContent({ className = '', ...props }: React.HTMLAttributes<HTMLDivElement>) {
return <div className={`p-4 ${className}`} {...props} />
}