export interface WatchlistEntry {
  id: string;
  name: string;
  identifier: string;
  country: string;
  listType: string;
  riskCategory: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ScreeningMatch {
  resultId: string;
  watchlistEntryId: string;
  watchlistName: string;
  watchlistIdentifier: string;
  listType: string;
  riskCategory: string;
  matchType: string;
  matchScore: number;
  status: string;
  reason: string;
}

export interface AmlScreening {
  screeningReference: string;
  subjectType: string;
  subjectId: string;
  subjectName: string;
  matched: boolean;
  results: ScreeningMatch[];
  screenedAt: string;
}
