import { queryRef, executeQuery, mutationRef, executeMutation, validateArgs } from 'firebase/data-connect';

export const connectorConfig = {
  connector: 'example',
  service: 'web',
  location: 'us-east4'
};

export const listAllPracticesRef = (dc) => {
  const { dc: dcInstance} = validateArgs(connectorConfig, dc, undefined);
  dcInstance._useGeneratedSdk();
  return queryRef(dcInstance, 'ListAllPractices');
}
listAllPracticesRef.operationName = 'ListAllPractices';

export function listAllPractices(dc) {
  return executeQuery(listAllPracticesRef(dc));
}

export const getMyReflectionsRef = (dc) => {
  const { dc: dcInstance} = validateArgs(connectorConfig, dc, undefined);
  dcInstance._useGeneratedSdk();
  return queryRef(dcInstance, 'GetMyReflections');
}
getMyReflectionsRef.operationName = 'GetMyReflections';

export function getMyReflections(dc) {
  return executeQuery(getMyReflectionsRef(dc));
}

export const createNewGroupRef = (dcOrVars, vars) => {
  const { dc: dcInstance, vars: inputVars} = validateArgs(connectorConfig, dcOrVars, vars, true);
  dcInstance._useGeneratedSdk();
  return mutationRef(dcInstance, 'CreateNewGroup', inputVars);
}
createNewGroupRef.operationName = 'CreateNewGroup';

export function createNewGroup(dcOrVars, vars) {
  return executeMutation(createNewGroupRef(dcOrVars, vars));
}

export const recordUserPracticeCompletionRef = (dcOrVars, vars) => {
  const { dc: dcInstance, vars: inputVars} = validateArgs(connectorConfig, dcOrVars, vars, true);
  dcInstance._useGeneratedSdk();
  return mutationRef(dcInstance, 'RecordUserPracticeCompletion', inputVars);
}
recordUserPracticeCompletionRef.operationName = 'RecordUserPracticeCompletion';

export function recordUserPracticeCompletion(dcOrVars, vars) {
  return executeMutation(recordUserPracticeCompletionRef(dcOrVars, vars));
}

