import { type FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { Card } from '../atoms/Card';
import { Button } from '../atoms/Button';
import { Input } from '../atoms/Input';
import { FileInput } from '../atoms/FileInput';
import { Badge } from '../atoms/Badge';
import { useCreateInquiry, useSubmitGovernmentId, useSubmitSelfie } from '../../hooks/useKyc';
import { VerificationStatus } from '../../constants/enums';
import { ROUTES } from '../../constants/routes';
import type { VerificationSummary } from '../../store/models/identity.model';

type Step = 'start' | 'government-id' | 'selfie' | 'done';

export function KycPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [step, setStep] = useState<Step>('start');
  const [inquiryId, setInquiryId] = useState<string | null>(null);
  const [nric, setNric] = useState('');
  const [dob, setDob] = useState('');
  const [nationality, setNationality] = useState('');
  const [govIdImage, setGovIdImage] = useState<File | null>(null);
  const [selfieImage, setSelfieImage] = useState<File | null>(null);
  const [lastResult, setLastResult] = useState<VerificationSummary | null>(null);

  const createInquiry = useCreateInquiry();
  const submitGovernmentId = useSubmitGovernmentId();
  const submitSelfie = useSubmitSelfie();

  const handleStart = () => {
    createInquiry.mutate(undefined, {
      onSuccess: (inquiry) => {
        setInquiryId(inquiry.inquiryId);
        setStep('government-id');
      },
    });
  };

  const handleGovernmentIdSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!inquiryId || !govIdImage) return;
    submitGovernmentId.mutate(
      { inquiryId, fields: { nric, dob, nationality }, image: govIdImage },
      {
        onSuccess: (result) => {
          setLastResult(result);
          if (result.status === VerificationStatus.PASSED) {
            setStep('selfie');
          }
        },
      }
    );
  };

  const handleSelfieSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!inquiryId || !selfieImage) return;
    submitSelfie.mutate(
      { inquiryId, image: selfieImage },
      {
        onSuccess: (result) => {
          setLastResult(result);
          if (result.status === VerificationStatus.PASSED) {
            queryClient.invalidateQueries({ queryKey: ['me'] });
            setStep('done');
          }
        },
      }
    );
  };

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold text-foreground">Identity Verification</h1>

      {step === 'start' && (
        <Card className="flex flex-col gap-4">
          <p className="text-foreground">
            Verify your identity to unlock all ManekPay features. You'll need a government ID and a selfie.
          </p>
          {createInquiry.isError && (
            <p role="alert" className="rounded bg-danger/10 px-3 py-2 text-sm text-danger">
              {createInquiry.error instanceof Error ? createInquiry.error.message : 'Could not start verification'}
            </p>
          )}
          <Button onClick={handleStart} isLoading={createInquiry.isPending}>
            Start verification
          </Button>
        </Card>
      )}

      {step === 'government-id' && (
        <Card>
          <form onSubmit={handleGovernmentIdSubmit} className="flex flex-col gap-4">
            <h2 className="text-lg font-medium text-foreground">Step 1: Government ID</h2>
            {lastResult && lastResult.status === VerificationStatus.FAILED && (
              <p role="alert" className="rounded bg-danger/10 px-3 py-2 text-sm text-danger">
                Verification failed: {lastResult.resultDetail ?? 'please check your details and try again.'}
              </p>
            )}
            {submitGovernmentId.isError && (
              <p role="alert" className="rounded bg-danger/10 px-3 py-2 text-sm text-danger">
                {submitGovernmentId.error instanceof Error ? submitGovernmentId.error.message : 'Submission failed'}
              </p>
            )}
            <Input label="NRIC (e.g. 900101-14-5678)" required value={nric} onChange={(e) => setNric(e.target.value)} />
            <Input label="Date of birth" type="date" required value={dob} onChange={(e) => setDob(e.target.value)} />
            <Input label="Nationality" required value={nationality} onChange={(e) => setNationality(e.target.value)} />
            <FileInput label="ID document photo" required onChange={setGovIdImage} />
            <Button type="submit" isLoading={submitGovernmentId.isPending}>
              Submit
            </Button>
          </form>
        </Card>
      )}

      {step === 'selfie' && (
        <Card>
          <form onSubmit={handleSelfieSubmit} className="flex flex-col gap-4">
            <h2 className="text-lg font-medium text-foreground">Step 2: Selfie</h2>
            {lastResult && lastResult.status === VerificationStatus.FAILED && (
              <p role="alert" className="rounded bg-danger/10 px-3 py-2 text-sm text-danger">
                Verification failed: {lastResult.resultDetail ?? 'please try again.'}
              </p>
            )}
            {submitSelfie.isError && (
              <p role="alert" className="rounded bg-danger/10 px-3 py-2 text-sm text-danger">
                {submitSelfie.error instanceof Error ? submitSelfie.error.message : 'Submission failed'}
              </p>
            )}
            <FileInput label="Selfie photo" required onChange={setSelfieImage} />
            <Button type="submit" isLoading={submitSelfie.isPending}>
              Submit
            </Button>
          </form>
        </Card>
      )}

      {step === 'done' && (
        <Card className="flex flex-col items-start gap-4">
          <Badge status="APPROVED" />
          <p className="text-foreground">Verification complete — you're all set.</p>
          <Button onClick={() => navigate(ROUTES.DASHBOARD)}>Back to Dashboard</Button>
        </Card>
      )}
    </div>
  );
}
