import { apiClient } from "@/lib/apiClient";

// All ai-usage endpoints live here. Components/hooks call these, never fetch directly.

// GET /ai-usage  (filter + Pageable) -> Spring Page<AIUsageLogResponse>
export function getUsageLogs(params, signal) {
  return apiClient.get("/ai-usage", { params, signal });
}

// GET /ai-usage/summary -> AIUsageSummaryResponse
export function getUsageSummary(signal) {
  return apiClient.get("/ai-usage/summary", { signal });
}

// GET /ai-usage/daily?days= -> AIUsageDailyResponse[], zero-filled, oldest first
export function getUsageDaily(days, signal) {
  return apiClient.get("/ai-usage/daily", { params: { days }, signal });
}

// GET /ai-usage/models -> string[], distinct models ever logged
export function getUsageModels(signal) {
  return apiClient.get("/ai-usage/models", { signal });
}
