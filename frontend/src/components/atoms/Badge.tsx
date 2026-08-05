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

export function Badge({ status }: BadgeProps) {
  return (
    <span className={`rounded-full px-3 py-1 text-xs font-medium ${STATUS_CLASSES[status]}`}>
      {status.replace('_', ' ')}
    </span>
  );
}
