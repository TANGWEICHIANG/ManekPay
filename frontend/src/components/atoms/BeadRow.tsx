interface BeadRowProps {
  /** Fraction complete, 0 to 1. */
  fraction: number;
  /** Total beads on the rod. */
  count?: number;
  colorClass?: string;
  className?: string;
}

// The counting frame's one functional data mark: discrete filled/empty beads standing in for
// a progress bar. Used where "how many of N" is the real fact - a goal's progress, a rule's
// threshold - never as a decorative stand-in for a number that already reads fine as a numeral.
export function BeadRow({ fraction, count = 10, colorClass = 'bg-primary', className = '' }: BeadRowProps) {
  const filled = Math.round(Math.min(Math.max(fraction, 0), 1) * count);
  return (
    <div className={`flex items-center gap-1 ${className}`} role="img" aria-label={`${Math.round(fraction * 100)}% complete`}>
      {Array.from({ length: count }, (_, i) => (
        <span
          key={i}
          className={`h-2 w-2 rounded-full border ${i < filled ? `${colorClass} border-transparent` : 'border-border bg-transparent'}`}
          aria-hidden="true"
        />
      ))}
    </div>
  );
}
