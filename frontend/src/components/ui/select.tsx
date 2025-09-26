import * as React from 'react'


type Ctx = {
value?: string
onValueChange?: (v: string) => void
}
const SelectCtx = React.createContext<Ctx>({})


export function Select(
{ value, onValueChange, children }: { value?: string; onValueChange?: (v: string) => void; children: React.ReactNode }
) {
// We render children as-is; the real select UI is inside SelectContent.
return (
<SelectCtx.Provider value={{ value, onValueChange }}>
<div className="inline-flex gap-2 items-center">{children}</div>
</SelectCtx.Provider>
)
}


export function SelectTrigger({ className = '', ...props }: React.HTMLAttributes<HTMLButtonElement>) {
// Placeholder (for API compatibility). Not used in this minimal implementation.
return <button className={`hidden ${className}`} aria-hidden {...props} />
}


export function SelectValue({ placeholder }: { placeholder?: string }) {
// Placeholder component; no-op in this minimal build.
return <span className="hidden" aria-hidden>{placeholder}</span>
}


export function SelectContent({ children, className = '' }: React.HTMLAttributes<HTMLDivElement>) {
const ctx = React.useContext(SelectCtx)
// Build <option> list from SelectItem children
const options: Array<{ value: string; label: React.ReactNode }> = []


React.Children.forEach(children as any, (child: any) => {
if (!child) return
if (child.type && child.type.displayName === 'SelectItem') {
options.push({ value: child.props.value, label: child.props.children })
}
})


return (
<div className={className}>
<select
className="border border-slate-300 rounded-xl px-3 py-2 text-sm"
value={ctx.value}
onChange={(e) => ctx.onValueChange?.(e.target.value)}
>
{options.map((o) => (
<option key={o.value} value={o.value}>{o.label}</option>
))}
</select>
</div>
)
}


export function SelectItem({ value, children }: { value: string; children: React.ReactNode }) {
// Acts as data for SelectContent to render <option>s
return <option value={value}>{children}</option> as any
}
SelectItem.displayName = 'SelectItem'