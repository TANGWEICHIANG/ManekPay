import { Stamp } from 'lucide-react';
import { KycStatus } from '../../constants/enums';

interface BadgeProps {
  status: KycStatus;
}

// APPROVED reads as a customs-clearance stamp, not a soft status pill - the one state this app
// treats as a mark of authority rather than a routine color chip.
const STATUS_CLASSES: Record<KycStatus, string> = {
  PENDING: 'border-warning/40 bg-warning/10 text-warning',
  IN_REVIEW: 'border-warning/40 bg-warning/10 text-warning',
  APPROVED: 'border-stamp/50 bg-stamp/10 text-stamp',
  REJECTED: 'border-danger/40 bg-danger/10 text-danger',
};

export function Badge({ status }: BadgeProps) {
  const isApproved = status === KycStatus.APPROVED;
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-sm border px-2.5 py-1 font-mono text-xs font-medium uppercase tracking-wide ${STATUS_CLASSES[status]}`}
    >
      {isApproved ? (
        <Stamp className="h-3 w-3 -rotate-12" strokeWidth={2.25} aria-hidden="true" />
      ) : (
        <span className="h-1.5 w-1.5 rounded-full bg-current" aria-hidden="true" />
      )}
      {status.replace('_', ' ')}
    </span>
  );
}
