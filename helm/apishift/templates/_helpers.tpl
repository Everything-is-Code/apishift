{{- define "apishift.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "apishift.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- printf "%s" $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{- define "apishift.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: apishift
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}

{{- define "apishift.datagrid" -}}
{{ include "apishift.fullname" . }}-datagrid
{{- end }}

{{- define "apishift.datagridHeadless" -}}
{{ include "apishift.fullname" . }}-datagrid-headless
{{- end }}

{{- define "apishift.datagridDnsQuery" -}}
{{ include "apishift.datagridHeadless" . }}.{{ .Release.Namespace }}.svc.cluster.local
{{- end }}

{{- define "apishift.datagridLabels" -}}
{{ include "apishift.labels" . }}
app.kubernetes.io/name: {{ include "apishift.datagrid" . }}
app.kubernetes.io/component: datagrid
{{- end }}

{{- define "apishift.datagridSelectorLabels" -}}
app.kubernetes.io/name: {{ include "apishift.datagrid" . }}
app.kubernetes.io/component: datagrid
{{- end }}
