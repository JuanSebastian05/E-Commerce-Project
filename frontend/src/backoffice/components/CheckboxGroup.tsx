export interface CheckboxOption {
  value: string
  label: string
  hint?: string
  disabled?: boolean
}

interface CheckboxGroupProps {
  legend: string
  options: CheckboxOption[]
  selected: string[]
  onChange: (selected: string[]) => void
  error?: string | null
}

export function CheckboxGroup({ legend, options, selected, onChange, error }: CheckboxGroupProps) {
  function toggle(value: string, checked: boolean) {
    onChange(checked ? [...selected, value] : selected.filter((item) => item !== value))
  }

  return (
    <fieldset className="space-y-2">
      <legend className="text-sm font-medium text-gray-700">{legend}</legend>
      <div className="flex flex-wrap gap-x-6 gap-y-2">
        {options.map((option) => (
          <label key={option.value} className="flex items-start gap-2 text-sm" title={option.hint}>
            <input
              type="checkbox"
              className="mt-0.5"
              checked={selected.includes(option.value)}
              disabled={option.disabled}
              onChange={(event) => toggle(option.value, event.target.checked)}
            />
            <span>{option.label}</span>
          </label>
        ))}
      </div>
      {error && <p className="text-sm text-red-600">{error}</p>}
    </fieldset>
  )
}
