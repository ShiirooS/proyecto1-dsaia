import { Link } from 'react-router-dom'

interface ButtonProps {
    content: string;
    className: string;
    to?: string;
  }

const classes = 'block rounded-lg bg-orange-200 px-5 py-2.5 text-sm font-medium text-orange-600 transition hover:text-orange-500'

export default function ButtonSecondary({ content, className, to }: ButtonProps) {
  if (to) {
    return <Link className={`${classes} ${className}`} to={to}>{content}</Link>
  }
  return (
    <a className={`${classes} ${className}`} href="#">
    {content}
    </a>
  )
}
