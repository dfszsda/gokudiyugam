import { ConnectorConfig, DataConnect, QueryRef, QueryPromise, MutationRef, MutationPromise } from 'firebase/data-connect';

export const connectorConfig: ConnectorConfig;

export type TimestampString = string;
export type UUIDString = string;
export type Int64String = string;
export type DateString = string;




export interface CreateNewGroupData {
  group_insert: Group_Key;
}

export interface CreateNewGroupVariables {
  name: string;
  type: string;
  description?: string | null;
  imageUrl?: string | null;
}

export interface GetMyReflectionsData {
  reflections: ({
    id: UUIDString;
    title: string;
    content: string;
    createdAt: TimestampString;
    isPrivate?: boolean | null;
    associatedPractice?: {
      id: UUIDString;
      title: string;
    } & Practice_Key;
  } & Reflection_Key)[];
}

export interface GroupMember_Key {
  userId: UUIDString;
  groupId: UUIDString;
  __typename?: 'GroupMember_Key';
}

export interface Group_Key {
  id: UUIDString;
  __typename?: 'Group_Key';
}

export interface ListAllPracticesData {
  practices: ({
    id: UUIDString;
    title: string;
    description: string;
    durationMinutes: number;
    type: string;
    difficultyLevel?: string | null;
    imageUrl?: string | null;
  } & Practice_Key)[];
}

export interface Practice_Key {
  id: UUIDString;
  __typename?: 'Practice_Key';
}

export interface RecordUserPracticeCompletionData {
  userPractice_insert: UserPractice_Key;
}

export interface RecordUserPracticeCompletionVariables {
  practiceId: UUIDString;
  durationPracticedMinutes: number;
  rating?: number | null;
  userNotes?: string | null;
}

export interface Reflection_Key {
  id: UUIDString;
  __typename?: 'Reflection_Key';
}

export interface UserPractice_Key {
  userId: UUIDString;
  practiceId: UUIDString;
  __typename?: 'UserPractice_Key';
}

export interface User_Key {
  id: UUIDString;
  __typename?: 'User_Key';
}

interface ListAllPracticesRef {
  /* Allow users to create refs without passing in DataConnect */
  (): QueryRef<ListAllPracticesData, undefined>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect): QueryRef<ListAllPracticesData, undefined>;
  operationName: string;
}
export const listAllPracticesRef: ListAllPracticesRef;

export function listAllPractices(): QueryPromise<ListAllPracticesData, undefined>;
export function listAllPractices(dc: DataConnect): QueryPromise<ListAllPracticesData, undefined>;

interface GetMyReflectionsRef {
  /* Allow users to create refs without passing in DataConnect */
  (): QueryRef<GetMyReflectionsData, undefined>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect): QueryRef<GetMyReflectionsData, undefined>;
  operationName: string;
}
export const getMyReflectionsRef: GetMyReflectionsRef;

export function getMyReflections(): QueryPromise<GetMyReflectionsData, undefined>;
export function getMyReflections(dc: DataConnect): QueryPromise<GetMyReflectionsData, undefined>;

interface CreateNewGroupRef {
  /* Allow users to create refs without passing in DataConnect */
  (vars: CreateNewGroupVariables): MutationRef<CreateNewGroupData, CreateNewGroupVariables>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect, vars: CreateNewGroupVariables): MutationRef<CreateNewGroupData, CreateNewGroupVariables>;
  operationName: string;
}
export const createNewGroupRef: CreateNewGroupRef;

export function createNewGroup(vars: CreateNewGroupVariables): MutationPromise<CreateNewGroupData, CreateNewGroupVariables>;
export function createNewGroup(dc: DataConnect, vars: CreateNewGroupVariables): MutationPromise<CreateNewGroupData, CreateNewGroupVariables>;

interface RecordUserPracticeCompletionRef {
  /* Allow users to create refs without passing in DataConnect */
  (vars: RecordUserPracticeCompletionVariables): MutationRef<RecordUserPracticeCompletionData, RecordUserPracticeCompletionVariables>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect, vars: RecordUserPracticeCompletionVariables): MutationRef<RecordUserPracticeCompletionData, RecordUserPracticeCompletionVariables>;
  operationName: string;
}
export const recordUserPracticeCompletionRef: RecordUserPracticeCompletionRef;

export function recordUserPracticeCompletion(vars: RecordUserPracticeCompletionVariables): MutationPromise<RecordUserPracticeCompletionData, RecordUserPracticeCompletionVariables>;
export function recordUserPracticeCompletion(dc: DataConnect, vars: RecordUserPracticeCompletionVariables): MutationPromise<RecordUserPracticeCompletionData, RecordUserPracticeCompletionVariables>;

