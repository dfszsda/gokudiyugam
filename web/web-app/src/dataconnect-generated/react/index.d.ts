import { ListAllPracticesData, GetMyReflectionsData, CreateNewGroupData, CreateNewGroupVariables, RecordUserPracticeCompletionData, RecordUserPracticeCompletionVariables } from '../';
import { UseDataConnectQueryResult, useDataConnectQueryOptions, UseDataConnectMutationResult, useDataConnectMutationOptions} from '@tanstack-query-firebase/react/data-connect';
import { UseQueryResult, UseMutationResult} from '@tanstack/react-query';
import { DataConnect } from 'firebase/data-connect';
import { FirebaseError } from 'firebase/app';


export function useListAllPractices(options?: useDataConnectQueryOptions<ListAllPracticesData>): UseDataConnectQueryResult<ListAllPracticesData, undefined>;
export function useListAllPractices(dc: DataConnect, options?: useDataConnectQueryOptions<ListAllPracticesData>): UseDataConnectQueryResult<ListAllPracticesData, undefined>;

export function useGetMyReflections(options?: useDataConnectQueryOptions<GetMyReflectionsData>): UseDataConnectQueryResult<GetMyReflectionsData, undefined>;
export function useGetMyReflections(dc: DataConnect, options?: useDataConnectQueryOptions<GetMyReflectionsData>): UseDataConnectQueryResult<GetMyReflectionsData, undefined>;

export function useCreateNewGroup(options?: useDataConnectMutationOptions<CreateNewGroupData, FirebaseError, CreateNewGroupVariables>): UseDataConnectMutationResult<CreateNewGroupData, CreateNewGroupVariables>;
export function useCreateNewGroup(dc: DataConnect, options?: useDataConnectMutationOptions<CreateNewGroupData, FirebaseError, CreateNewGroupVariables>): UseDataConnectMutationResult<CreateNewGroupData, CreateNewGroupVariables>;

export function useRecordUserPracticeCompletion(options?: useDataConnectMutationOptions<RecordUserPracticeCompletionData, FirebaseError, RecordUserPracticeCompletionVariables>): UseDataConnectMutationResult<RecordUserPracticeCompletionData, RecordUserPracticeCompletionVariables>;
export function useRecordUserPracticeCompletion(dc: DataConnect, options?: useDataConnectMutationOptions<RecordUserPracticeCompletionData, FirebaseError, RecordUserPracticeCompletionVariables>): UseDataConnectMutationResult<RecordUserPracticeCompletionData, RecordUserPracticeCompletionVariables>;
