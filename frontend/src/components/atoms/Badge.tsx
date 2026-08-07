import { KycStatus } from '../../constants/enums';

interface BadgeProps {
  status: KycStatus;
}

const STATUS_CLASSES: Record<KycStatus, string> = {
  PENDING: 'bg-warning/10 text-warning',
  IN_REVIEW: 'bg-warning/10 text-warning',
  APPROVED: 'bg-success/10 text-success',
  REJECTED: 'bg-danger/10 text-danger',
};

const DOT_CLASSES: Record<KycStatus, string> = {
  PENDING: 'bg-warning',
  IN_REVIEW: 'bg-warning',
  APPROVED: 'bg-success',
  REJECTED: 'bg-danger',
};

export function Badge({ status }: BadgeProps) {
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold ${STATUS_CLASSES[status]}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${DOT_CLASSES[status]}`} aria-hidden="true" />
      {status.replace('_', ' ')}
    </span>
  );
}
