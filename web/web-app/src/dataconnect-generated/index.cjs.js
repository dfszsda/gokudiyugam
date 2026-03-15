const { queryRef, executeQuery, mutationRef, executeMutation, validateArgs } = require('firebase/data-connect');

const connectorConfig = {
  connector: 'example',
  service: 'web',
  location: 'us-east4'
};
exports.connectorConfig = connectorConfig;

const listAllPracticesRef = (dc) => {
  const { dc: dcInstance} = validateArgs(connectorConfig, dc, undefined);
  dcInstance._useGeneratedSdk();
  return queryRef(dcInstance, 'ListAllPractices');
}
listAllPracticesRef.operationName = 'ListAllPractices';
exports.listAllPracticesRef = listAllPracticesRef;

exports.listAllPractices = function listAllPractices(dc) {
  return executeQuery(listAllPracticesRef(dc));
};

const getMyReflectionsRef = (dc) => {
  const { dc: dcInstance} = validateArgs(connectorConfig, dc, undefined);
  dcInstance._useGeneratedSdk();
  return queryRef(dcInstance, 'GetMyReflections');
}
getMyReflectionsRef.operationName = 'GetMyReflections';
exports.getMyReflectionsRef = getMyReflectionsRef;

exports.getMyReflections = function getMyReflections(dc) {
  return executeQuery(getMyReflectionsRef(dc));
};

const createNewGroupRef = (dcOrVars, vars) => {
  const { dc: dcInstance, vars: inputVars} = validateArgs(connectorConfig, dcOrVars, vars, true);
  dcInstance._useGeneratedSdk();
  return mutationRef(dcInstance, 'CreateNewGroup', inputVars);
}
createNewGroupRef.operationName = 'CreateNewGroup';
exports.createNewGroupRef = createNewGroupRef;

exports.createNewGroup = function createNewGroup(dcOrVars, vars) {
  return executeMutation(createNewGroupRef(dcOrVars, vars));
};

const recordUserPracticeCompletionRef = (dcOrVars, vars) => {
  const { dc: dcInstance, vars: inputVars} = validateArgs(connectorConfig, dcOrVars, vars, true);
  dcInstance._useGeneratedSdk();
  return mutationRef(dcInstance, 'RecordUserPracticeCompletion', inputVars);
}
recordUserPracticeCompletionRef.operationName = 'RecordUserPracticeCompletion';
exports.recordUserPracticeCompletionRef = recordUserPracticeCompletionRef;

exports.recordUserPracticeCompletion = function recordUserPracticeCompletion(dcOrVars, vars) {
  return executeMutation(recordUserPracticeCompletionRef(dcOrVars, vars));
};
