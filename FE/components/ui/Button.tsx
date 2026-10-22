import * as React from "react"
import { cn } from "@/lib/utils"

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "default" | "outline" | "ghost" | "link" | "accent" | "danger"
  size?: "default" | "sm" | "lg" | "icon"
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = "default", size = "default", ...props }, ref) => {
    return (
      <button
        ref={ref}
        className={cn(
          "inline-flex items-center justify-center whitespace-nowrap rounded-md text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50",
          {
            "bg-navy-900 text-white shadow hover:bg-navy-800": variant === "default",
            "border border-border bg-white shadow-sm hover:bg-slate-50 text-slate-700": variant === "outline",
            "hover:bg-slate-100 text-slate-700": variant === "ghost",
            "text-navy-900 underline-offset-4 hover:underline": variant === "link",
            "bg-teal-600 text-white shadow hover:bg-teal-700": variant === "accent",
            "bg-red-500 text-white shadow hover:bg-red-600": variant === "danger",
            
            "h-9 px-4 py-2": size === "default",
            "h-8 rounded-md px-3 text-xs": size === "sm",
            "h-10 rounded-md px-8": size === "lg",
            "h-9 w-9": size === "icon",
          },
          className
        )}
        {...props}
      />
    )
  }
)
Button.displayName = "Button"

export { Button }
