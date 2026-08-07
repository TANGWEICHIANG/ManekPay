import { type ChangeEvent, useId, useState } from 'react';

interface FileInputProps {
  label: string;
  onChange: (file: File | null) => void;
  required?: boolean;
}

export function FileInput({ label, onChange, required }: FileInputProps) {
  const id = useId();
  const [fileName, setFileName] = useState<string | null>(null);

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    setFileName(file?.name ?? null);
    onChange(file);
  };

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-medium text-foreground">
        {label}
      </label>
      <input
        id={id}
        type="file"
        accept="image/*"
        required={required}
        onChange={handleChange}
        className="rounded-md border border-border bg-surface px-3 py-2 text-sm text-foreground transition-colors duration-fast hover:border-muted file:mr-3 file:rounded-sm file:border-0 file:bg-primary file:px-3 file:py-1 file:font-semibold file:text-background"
      />
      {fileName && <span className="text-sm text-muted">{fileName}</span>}
    </div>
  );
}
